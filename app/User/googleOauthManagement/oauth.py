import os
import json
import flask
import requests
from flask import redirect

app = flask.Flask(__name__)

CLIENT_ID = os.environ.get('CLIENT_ID')
CLIENT_SECRET = os.environ.get('CLIENT_SECRET')
SCOPE = 'https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email'
REDIRECT_URI = 'http://localhost:8080/callback'


@app.route('/oauth/google')
def index():
    if 'credentials' not in flask.session:
        return flask.redirect(flask.url_for('callback'))
    credentials = json.loads(flask.session['credentials'])
    if credentials['expires_in'] <= 0:
        return flask.redirect(flask.url_for('callback'))
    else:
        access_token = credentials['access_token']
        headers = {'Authorization': 'Bearer {}'.format(access_token)}
        req_uri = 'https://www.googleapis.com/oauth2/v1/userinfo'
        r = requests.get(req_uri, headers=headers)
        if r.status_code == 200:
            user_info_json = r.json()
            email = user_info_json.get('email')
            name = user_info_json.get('name')
            user_info = {'email': email, 'name': name}
            url = "http://host.docker.internal:6039/UserOauth"
            response = requests.post(url, json=user_info)
            if response.status_code == 200:
                url = "http://host.docker.internal:3000/PersonalPageUser/"
                user_id = response.text
                app.logger.info(f'Oauth OK. User ID: {user_id}')
                url = url + user_id
                return redirect(url, 307)
            else:
                url = "http://host.docker.internal:3000/LoginUser.html?err=990"
                app.logger.error('Oauth KO')
                return redirect(url, 307)
        else:
            url = "http://host.docker.internal:3000/LoginUser.html?err=990"
            app.logger.error('Failed to retrieve user info from Google')
            return redirect(url, 307)

@app.route('/callback')
def callback():
    if 'code' not in flask.request.args:
        auth_uri = ('https://accounts.google.com/o/oauth2/v2/auth?response_type=code'
                    '&client_id={}&redirect_uri={}&scope={}').format(CLIENT_ID, REDIRECT_URI, SCOPE)
        return flask.redirect(auth_uri)
    else:
        auth_code = flask.request.args.get('code')
        data = {'code': auth_code,
                'client_id': CLIENT_ID,
                'client_secret': CLIENT_SECRET,
                'redirect_uri': REDIRECT_URI,
                'grant_type': 'authorization_code'}
        r = requests.post('https://oauth2.googleapis.com/token', data=data)
        flask.session['credentials'] = r.text
        return flask.redirect(flask.url_for('index'))


if __name__ == '__main__':
    import uuid
    app.secret_key = str(uuid.uuid4())
    app.debug = False
    app.run(debug=True, host='0.0.0.0', port=8080)

