# Keras OCR android application

ML model is based on [Keras implementation](https://github.com/kurapan/CRNN) of Convolutional Recurrent Neural Network for text recognition.
The CRNNs are the combination of two of the most prominent neural networks. They involve CNN(convolutional neural network) followed 
by the RNN(Recurrent neural networks). The proposed network is similar to the CRNN but generates better or optimal results especially
towards audio signal processing and text recognition.

<img src="CRNN.png" width="2560" height="240">

In our example model consists of Conv2D layers and unidirectional LSTM layers.

# Explore the code

We're now going to walk through the most important parts of the sample code.

## General usage
On screen there is a horizontal RecyclerView that displays colored images with text. The background and the text are of different random colors.
User picks on of the images and then 2 procedures start. First, the image is converted to graysclae, then one of the channels is isolated to generate 
the bytebuffer and lastly the flex delegate performs inference. Second, [ML Kit's Textrecognition](https://developers.google.com/ml-kit/vision/text-recognition/android#kotlin) is used to perform OCR on the same grayscale image.
When the two procedures stop user watch o screen the output and the total inference time of the procedures.





