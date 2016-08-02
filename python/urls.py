
from flask import redirect, url_for

def getIndexURL():
	return redirect(url_for('index'))

def getLoginURL():
	return redirect(url_for('login'))

def getCurrentURL(formData):
	if 'current' in formData and formData['current'] != '':
		current = formData['current']
		url = None
		if current == "server":
			url = url_for(current, nameOwner = formData['nameOwner'], nameServer = formData['nameServer'])
		else:
			url_for(current)
		return redirect(url)
	return getIndexURL()

def getUserURL(username):
	return redirect(url_for('user', username = username))

def getUserSettingsURL(username):
	return redirect(url_for('userSettings', username = username))
