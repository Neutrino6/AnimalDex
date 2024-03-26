import tensorflow as tf
import numpy as np
from tensorflow.keras.applications.mobilenet_v2 import MobileNetV2, preprocess_input, decode_predictions
from tensorflow.keras.preprocessing import image
from flask import Flask, request, jsonify
import io

app = Flask(__name__)

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
    if 'fileInput' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    
    file = request.files['fileInput']
    
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    img_bytes = io.BytesIO(file.read())

    prediction = predict_image(img_bytes)
    
    if not prediction:
        return jsonify({'error': 'No prediction found'}), 400
    
    result = {'prediction': prediction[0][1], 'probability': float(prediction[0][2])}

    return jsonify(result), 200


if __name__ == '__main__':
    app.run(debug=True)

