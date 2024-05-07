FROM python
COPY . .
RUN pip install tensorflow numpy Flask pillow requests
EXPOSE 5000
ENTRYPOINT ["python"]
CMD ["recognition.py"]