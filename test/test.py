
import urllib2
import json
import requests

data = {
	'package': 'core-linux_headless64',
	'from': '0.12.10',
	'to' : '0.12.11',
	'apiVersion': 2
}

req = requests.get('https://www.factorio.com/updater/get-download-link', params=data)
if req.status_code == 200:
	url = req.json()[0]
	filename = url.split('?')[0].split('/')[-1]
	print(url)
	print(filename)
