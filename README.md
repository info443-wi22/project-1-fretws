# Architecture of a Seeing-eye Application

This code base is for an Android Application called 'Rekognition.' Developed by Will Song, Tom Nguyen, and myself, this application allows users to take pictures and have any text or objects detected in the image read aloud to them. This is intended to be used by those with vision impairments, and facilitates this by reading detected text and object labels aloud to the user.

## Structure Overview

### Top level architecture of the application

Stores are the logic layer that bridges between the UI and the Device Storage, and Services are the logic layer that bridges to the Firebase backend.
Stores encapsulate choices about what information to store about the images and how to store it.

Services encapsulate choices about what Firebase endpoint to use and when. Requests to Firebase are made directly by the Services, and therefore the Services package must also encapsulate information about the structure of queries and responses made to and from Firebase.

Activities are composed of Fragments, and Adapters are used by certain fragments to populate UI list items with data.

![architecture diagram](/imgs/Top_Level_Architecture.png)

### More Detailed architecture of the application

ViewModels act as the subject in a subject-observer pattern. The Fragments and Stores observe and initiate changes in the Viewmodels.

![architecture diagram](/imgs/Detailed_Architecture.png)

### Sequence diagram showing how an image of text goes from being captured by the user to being read aloud

Each step in the sequence is labelled with its number in the sequence.
Self-connections such as 3, 4, and 14 are all internal processing done by their corresonding component. While these are not the only internal steps that occur in this sequence, these are the internal steps that are necessary for understanding the sequence as a whole.
<!-- <img src="https://user-images.githubusercontent.com/62970170/150383233-6d5f1bfc-9510-489e-bfdf-7942a73f9eaf.png" width="1600" height="1080"> -->
![architecture diagram](/imgs/Image_Capture_Sequence.png)

The part of the architecture that we will focus on most is the Services package. Specifically, FirebaseFunctionsService -- the service that handles the complexity of calling the right endpoint of the Firebase API for each image and processing Json responses from the API into a more usable form.

## Testing

### How to run the tests

Run `./gradlew test` from the `rekognition` directory. The first time I ran it, this took quite some time to run and I had to run it again for any tests to actually execute.
I also highly recommend using powershell to run this command, because if you use git bash like I usually would, there are odd control characters that make it almost unreadable.
If this doesn't work or you already have Android Studio, then in Android Studio you can hit 'Ctrl+Shift+F10' or right click on the recognition directory and run all tests from the context menu.

### Code Coverage

![Code Coverage Report](/imgs/Testing_Coverage.png)

The missing code that is being reported as uncovered is all responsible for communication with the firebase backend, and therefore will not be included in automated testing.
Including these lines of code in automated testing would be possible if the firebase backend were able to be emulated locally, but since this specific backend utilizes Cloud Vision AI, it cannot be emulated locally (at least not without drastic changes to the code base).

## Refactoring

### Testability
Many parts of the services package and its immediate clients used chains of getters instead of temporary variables, which leads to fewer lines of code, a cleaner look, and in the end, poor debuggability.
These were my first targets for refactoring, largely because it was necessary to make these refactorings in order to develop a test suite.

Also for the purpose of testability, I was forced to change visibility of certain functions from `private` to `internal`.

### `requestAnnotation()` and `formatAnnotationResult()`

The rest of the Refactoring section is devoted to specific cases of refactoring that had meaningful impact on package architecture, including readability, flexibility, and maintainability.

``` Kotlin
fun requestAnnotation(image: Bitmap): AnnotateImageResponse {
    result = Firebase.functions.call(image) // pseudocode
    ...
    return Gson().fromJson(result.asArray.first(), AnnotateImageResponse::class.java)
}
```

The above cannot be tested for its formatting of the firebase response, so I extracted a function that can be tested

``` Kotlin
fun requestAnnotation(image: Bitmap): AnnotateImageResponse {
    result = Firebase.functions.call(image) // pseudocode
    ...
    return formatAnnotationResult(result)
}
fun formatAnnotationResult(result: JsonElement): AnnotateImageResponse {
    return Gson().fromJson(result.asJsonArray.first(), AnnotateImageResponse::class.java)
}
```

But as it turns out, even this was not testable because `Gson().fromJson()` was breaking the null safety of a field of the AnnotateImageResponse, and I could not replicate this easily in tests. So, by testing this portion of the code, the broken null safety was revealed and I repaired it immediately. The following uses the elvis operator (written as `?:`) to detect and replace null values generated by parsing Json -> Java -> Kotlin.

``` Kotlin
fun requestAnnotation(image: Bitmap): AnnotateImageResponse {
    result = Firebase.functions.call(image) // pseudocode
    ...
    return formatAnnotationResult(result)
}
fun formatAnnotationResult(result: JsonElement): AnnotateImageResponse {
    val nullableDataResponse = Gson().fromJson(result.asJsonArray.first(), AnnotateImageResponse::class.java)
    val nonNullable = AnnotateImageResponse(
        fullTextAnnotation = nullableDataResponse.fullTextAnnotation,
        labelAnnotations = nullableDataResponse.labelAnnotations
            ?: List<EntityAnnotation>(size = 0, init = {EntityAnnotation("", 0.0)})
    )
    return nonNullable
}
```

There is also that chain of getters used to get the data we need out of the raw Json returned by Firebase. This should be extracted into a well-named intermediate variable in order to better explain what that chain is doing and also make it more debuggable; it is difficult to access the value of the chain when it is immediately passed to `Gson().fromJson(...)`.

``` Kotlin
internal fun formatAnnotationResult(resultsArray: JsonElement): AnnotateImageResponse {
    // We only call for one image at a time, so there will only ever be one result in the array
    val soleAnnotation = resultsArray.asJsonArray.first()
    val nullableDataResponse = Gson().fromJson(soleAnnotation, AnnotateImageResponse::class.java)
    val nonNullable = AnnotateImageResponse(
        fullTextAnnotation = nullableDataResponse.fullTextAnnotation,
        labelAnnotations = nullableDataResponse.labelAnnotations
            ?: List<EntityAnnotation>(size = 0, init = {EntityAnnotation("", 0.0)})
    )
    return nonNullable
}
```

The final refactoring of this portion of code took me a little while to notice. In fact, only when I started to describe the FirebaseFunctionsService did I realize it had two distinct parts of its behavior that can be separated into two different classes.
The first part of its behavior, which will remain as part of FirebaseFunctionsService is to handle the complexity of calling the right endpoint of the Firebase API for each image.
The second part, which we will extract into a separate class, is processing Json responses from the API into a more usable form.
There are some data classes used in parsing responses that move to the newly created module `RecognitionJsonResponse`, but the more important parts of this refactoring are a new function and a change in `requestAnnotation`.
This is what `requestAnnotation` looked like before this final refactoring:

``` Kotlin
internal suspend fun requestAnnotation(requestBody: JsonObject): AnnotateImageResponse {
    ...
    val resultsArray = suspendCoroutine<JsonElement> { continuation ->
        functions
            .getHttpsCallable("annotateImage")
            .call(requestBody.toString())
            .addOnSuccessListener { successfulResponse ->
                continuation.resume(
                    JsonParser.parseString(Gson().toJson(successfulResponse.data))
                )
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
    return formatAnnotationResult(resultsArray)
}
```

The specifics aren't necessary to understand other than the fact that whatever is passed to `continuation.resume()` is the value assigned to `resultsArray`. This is changed to the following:

``` Kotlin
  .addOnSuccessListener { successfulResponse ->
      continuation.resume(
          RecognitionJsonResponse.parseString(successfulResponse)
      )
  }
```

Now, FirebaseFunctionsService is agnostic of the details of parsing the https response, and the RecognitionJsonResponse module can be easily swapped out for a different implementation.
The new class looks like this:

``` Kotlin
object RecognitionJsonResponse {
  // This function is not very easy to test, so we do not combine it with `toKotlin`
    fun parseString(successfulResponse: HttpsCallableResult): AnnotateImageResponse {
        val parsedJsonElement = JsonParser.parseString(Gson().toJson(successfulResponse.data))
        return toKotlin(parsedJsonElement)
    }

    // This function is the same as before, it was just renamed from `formatAnnotationResult` to `toKotlin`. So, the test we have for it is almost unchanged
    internal fun toKotlin(resultsData: JsonElement) : AnnotateImageResponse { ... }
}
```

### `getAnnotatorByIdentifierText()` and `getIdentifierText()`

The following code directs captured photos to the endpoint corresponding to the selected tab; 'Text' or 'Object.'
This code uses the position of a tab to identify which endpoint to call. `when` in Kotlin is called a `switch` in other languages.

``` Kotlin
captureButton.setOnClickListener {
    when (optionsTab.selectedTabPosition) {
        0 -> takePhoto(FirebaseFunctionsService.Annotator.TEXT)
        1 -> takePhoto(FirebaseFunctionsService.Annotator.OBJECT)
        else -> Log.e(TAG, "Selected tab not found")
    }
}
```

I argue that it is better to use user-facing strings to identify which endpoint to call, because users aren't selecting the Text endpoint by choosing the 'left' tab;
they are selecting the Text endpoint by choosing the tab labeled 'Text.'
So, I refactored the code to compare the user-facing strings that are encapsulated within each tab to the same strings encapsulated within each endpoint.

``` Kotlin
captureButton.setOnClickListener {
    val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition) 
    when (selectedTab?.text) {
        Annotator.TEXT.getIdentifierText(requireContext()) -> takePhoto(FirebaseFunctionsService.Annotator.TEXT)
        Annotator.OBJECT.getIdentifierText(requireContext()) -> takePhoto(FirebaseFunctionsService.Annotator.OBJECT)
        else -> Log.e(TAG, "Selected tab not found")
    }
}
```

Well, that's kind of ugly. There *must* be a way to encapsulate this logic within the enum class members TEXT and OBJECT.
The first step to doing that is actually a documented type refactoring called 'Replace Conditional with Polymorphism,' where each member of an enum class is doing work that could be extracted into a function that belongs to each enum class member.
Instead of using a 'when' statement, we can iterate over each member and use polymorphism -- because all members implement the same interface -- to call `getIdentifierText` on each.

``` Kotlin
captureButton.setOnClickListener {
    val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition) 
    var currentAnnotator: Annotator;
    loop@ for (annotator in Annotator.values()) {
        if (tabText == annotator.getIdentifierText(requireContext())) {
            currentAnnotator = annotator
            break@loop
        }
    }
    if (currentAnnotator != null) takePhoto(currentAnnotator)
    else Log.e(TAG, "Selected tab ${selectedTab?.text} not matched to a computer vision endpoint")
}
```

Now, all that is left is to extract a function for getting the correct annotator and put it in the same module as the Annotator enum class.
We extract a function because it encapsulates and explains this piece of behavior.
The resultant function is a point of communication to the Annotator enum that relies on knowledge of its inner workings.
This justifies the function's new home.

``` Kotlin
class AccessibilityFragment : ... {
    ...
    captureButton.setOnClickListener {
        val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)
        val currentAnnotator = FirebaseFunctionsService.getAnnotatorForSelectedTab(requireContext(), tabLayout)
        if (currentAnnotator != null) takePhoto(currentAnnotator)
        else Log.e(TAG, "Selected tab ${selectedTab?.text} not matched to a computer vision endpoint")
    }
}
```

``` Kotlin
class FirebaseFunctionsService {
    ...
    fun getAnnotatorForSelectedTab(context: Context, tabLayout: TabLayout): Annotator? {
        val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)
        for (annotator in Annotator.values()) {
            if (selectedTab?.text == annotator.getIdentifierText(context)) {
                return annotator
            }
        }
        return null

    }
    enum class Annotator : AnnotatorInterface {
        ...
    }
}
```

While one could pass in `selectedTab.text`, it is preferable to pass in the TabLayout itself because this further hides the inner workings of the Annotator enumeration.
This way, the AccessibilityFragment does not know how the correct annotator is chosen, it just knows that the TabLayout and context are necessary.
This should minimize refactoring in the event that a different implementation of getAnnotatorForSelectedTab is adopted in the future.
