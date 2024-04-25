FROM python
COPY . .
RUN pip install Flask requests
EXPOSE 8080
ENTRYPOINT ["python"]
CMD ["oauth.py"]