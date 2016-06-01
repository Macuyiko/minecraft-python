import http.server

PORT = 8000
print ("serving at port", PORT)
server_address = ('', PORT)
httpd = http.server.HTTPServer(server_address, http.server.SimpleHTTPRequestHandler)
httpd.serve_forever()
