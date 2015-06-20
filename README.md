D3Backend
====

This is (or will be) the backend server used by us (Double Door Development) to control our Minecraft servers.

*We are not responsible for lost worlds, hacked accounts or destroyed servers.*

Help?!
------

**Protip:** There are commands now! (Use "help" to get started)

Bugs
----

Typos? [Go here.](https://github.com/DoubleDoorDevelopment/D3Backend/issues/10)

1. We use the Github issue tracker.
1. Please always provide a screenshot, stackstrace or log of the bug. **USE PASTEBIN(or similar) PLEASE!**
1. Tell us which page you where on (if applicable)
1. Tell us what you clicked exactly (if applicable)
1. If we ask you for more information and you don't replay within 14 days, the issue will be considerer inactive.

ToDO
----

- [ ]  Implement email system
- [ ]  Implement a server wrapper
    - [ ] Shut the actual server down if empty, restart if someone connects.
    - [ ] Keep clients connected during server restart
- [ ]  Multi 'node' support
- [ ]  Add a 'look and feel' config (change name, add bootstrap theme, ...)
- [ ]  In release: Use minified js and css

HttpS
-----

Step 1: Make a jks file.<br>
    Option 1: [Self signed certificate](https://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html) Please use this for internal network or testing only.<br>
    Option 2: [Proper, CA signed certificate](https://docs.oracle.com/cd/E19798-01/821-1751/ghlgv/index.html)<br>
**Protip:** [CA signed certificates don't have to be expensive...](https://www.startssl.com/)<br>

Step 2: Put the path (relative to run dir or absolute) and password in the config file.

Step 3: Restart the backend. Done.

License
-------

    Copyright (C) 2015  Dries007 & Double Door Development

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

We would like to draw your attention to this section of the GNU Affero General Public License v3.0:

    If your software can interact with users remotely through a computer
    network, you should also make sure that it provides a way for users to
    get its source.  For example, if your program is a web application, its
    interface could display a "Source" link that leads users to an archive
    of the code.  There are many ways you could offer source, and different
    solutions will be better for different programs; see section 13 for the
    specific requirements.
