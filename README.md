# Architecture of a Seeing-eye Application

This code base is for an Android Application called 'Rekognition.' Developed by Will Song, Tom Nguyen, and myself, this application allows users to take pictures and have any text or objects detected in the image read aloud to them. This is intended to be used by those with vision impairments.

## Structure Diagrams

### Top level architecture of the application

![architecture diagram](/imgs/Top_Level_Architecture.png)

### More Detailed architecture of the application

![architecture diagram](/imgs/Detailed_Architecture.png)

### Sequence diagram showing how an image of text goes from being captured by the user to being read aloud:

Using an img tag with a width of 1600px and a height of 1080px did not change the readability of the diagram. Instead, click on the image to get a better viewing experience.
<!-- <img src="https://user-images.githubusercontent.com/62970170/150383233-6d5f1bfc-9510-489e-bfdf-7942a73f9eaf.png" width="1600" height="1080"> -->
![architecture diagram](/imgs/Image_Capture_Sequence.png)

## Testing

### How to run the tests

I have compiled a testing configuration and pushed it to this repo. It relies on Android Studio to be run, and in Android Studio, you can click 'Add Configurations' > 'Gradle' > 'FirebaseFunctionsServiceTestSuite' > 'OK' in order to access this configuration.
If you do not wish to use android studio, you can run `./gradlew test` from the `rekognition` directory, though this took quite some time to run the first time I ran it, and I do not know how to make it run with coverage.
