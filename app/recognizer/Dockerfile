FROM python
COPY . .
RUN pip install tensorflow numpy Flask
RUN pip install pillow
EXPOSE 5000
ENTRYPOINT ["python"]
CMD ["recognition.py"]