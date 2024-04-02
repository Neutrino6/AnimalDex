import logging
import tensorflow as tf
import numpy as np
from tensorflow.keras.applications.mobilenet_v2 import MobileNetV2, preprocess_input, decode_predictions
from tensorflow.keras.preprocessing import image
from flask import Flask, request, jsonify
from flask import redirect
import requests
import io

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.DEBUG)  # Set logging level to DEBUG

model = MobileNetV2(weights='imagenet')

def predict_image(img):
    img = image.load_img(img, target_size=(224, 224))
    img_array = image.img_to_array(img)
    img_array = np.expand_dims(img_array, axis=0)
    img_array = preprocess_input(img_array)

    predictions = model.predict(img_array)
    
    decoded_predictions = decode_predictions(predictions, top=1)[0]  # Top 1 prediction

    return decoded_predictions

@app.route('/predict', methods=['POST'])
def predict():
    app.logger.info('Received a prediction request')  # Log an informational message
    
    if 'fileInput' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    
    file = request.files['fileInput']
    
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    img_bytes = io.BytesIO(file.read())

    prediction = predict_image(img_bytes)
    
    if not prediction:
        return jsonify({'error': 'No prediction found'}), 400
    
    # Sending the POST request with the prediction result back to the same endpoint
    if float(prediction[0][2]) > 0.65: # set a threshold value
        url="http://host.docker.internal:8080/certificates/upload?data="+prediction[0][1] # change to localhost when executing without docker
        response = requests.post(url)
        # Checking the response
        if response.status_code == 200:
            # If the request was successful, return the prediction result
            app.logger.info('Prediction result sent successfully')
            return redirect(url, code=307)
        else:
            # If there was an error, return an error message
            app.logger.error('Failed to send prediction result back: %s', response.text)
            return jsonify({'error': 'Failed to send prediction result back'}), 500
    else:
        url="http://host.docker.internal:8080/certificates/upload?data=UNRECOGNIZED" # change to localhost when executing without docker
        response = requests.post(url)
        return redirect(url, code=307)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
