#!/usr/bin/env python

import psutil
import json

def findBytes(bytes):
	return bytes / pow(1024, 2)

mem = psutil.virtual_memory()
data = { "total": findBytes(mem.total), "number": findBytes(mem.used) }
with open('static/run/ramUsage.json', 'w') as outfile:
	json.dump(data, outfile)
