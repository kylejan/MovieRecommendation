from flask import Flask, render_template, url_for, request
import happybase
import json

app = Flask(__name__)

@app.route('/')
def homepage():
        return render_template('index.html', movieList=None)

@app.route('/query', methods=['GET'])
def query():
        userID = request.args.get('userID', '')

        conn = happybase.Connection('student3-x2')
        table = conn.table('MovieRecom')
        row = table.row(userID)

        movieList = json.loads(row['mr:recommend'])

        return render_template('index.html', movieList=movieList)

if __name__ == '__main__':
    app.run(host='0.0.0.0')
~