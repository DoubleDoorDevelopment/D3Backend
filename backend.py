
import os

from flask import Flask, render_template, session, redirect, url_for, request
from functools import wraps

app = Flask(__name__)
app.config.from_object('config')

def login_required(f):
	@wraps(f)
	def decorated_function(*args, **kwargs):
		if not 'username' in session:
			return getLoginURL()
		return f(*args, **kwargs)
	return decorated_function

def init():
	app.secret_key = os.urandom(20)

def getIndexURL():
	return redirect(url_for('index'))

def getLoginURL():
	return redirect(url_for('login'))

@app.route('/', methods=['GET', 'POST'])
def index():
	return render_template("pages/Homepage.html", current="index",
		time_online = parse_minutes(7471),
		servers_total = 4,
		servers_online = 3,
		ram_total = 512,
		ram_usage = 126,
		users_total = 5,
		users_online = 2,
		players_online = 18,
		players_total = 50
	)

@app.route('/add/server', methods=['POST'])
@login_required
def addServer():
	print("test")
	print('current' in request)
	#request['serverName']
	print(request['current'])
	return redirect(url_for(request['current']))

@app.route('/add/user', methods=['POST'])
@login_required
def addUser():
	#request['username']
	return redirect(url_for(request['current']))

@app.route('/login', methods=['GET', 'POST'])
def login():
	if 'username' in session:
		return getIndexURL()
	elif request.method == 'POST':
		try:
			user = request.form['username']
			pw = request.form['password']
			valid = True # TODO
			if valid != True:
				session["error"] = valid
			if app.debug == True or valid == True:
				session['username'] = user
				session['displayName'] = session['username']
				session['isAdmin'] = False
				#session["error"] = "You do not have access"
			
			# Send user back to index page
			# (if username wasnt set, it will redirect back to login screen)
			return getIndexURL()
			
		except Exception as e:
			return str(e)

@app.route('/logout')
@login_required
def logout():
	session.pop('username', None)
	session.pop('displayName', None)
	return getIndexURL()

@app.errorhandler(404)
def not_found(error):
	return render_template('page/404.html'), 404

def parse_minutes(time):
	minutes = time % 60
	time = int(time / 60)
	hours = time % 24
	time = int(time / 24)
	return str(time) + " days " + str(hours) + " hours " + str(minutes) + " minutes"

init()

if __name__ == '__main__':
	ctx = app.test_request_context()
	ctx.push()
	app.preprocess_request()
	port = int(os.getenv('PORT', 8080))
	host = os.getenv('IP', '0.0.0.0')
	app.run(port=port, host=host)
