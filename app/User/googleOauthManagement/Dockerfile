FROM python
COPY . .
RUN pip install Flask requests

ENV CLIENT_ID=894483498590-vce84rh69ulm5hvbckqs507d6biu9q3r.apps.googleusercontent.com
ENV CLIENT_SECRET=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

EXPOSE 8080
ENTRYPOINT ["python"]
CMD ["oauth.py"]