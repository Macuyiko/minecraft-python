WEB_SOCKET_SWF_LOCATION = "WebSocketMain.swf";
WEB_SOCKET_DEBUG = true;
WEB_SOCKET = undefined;
JQCONSOLE = undefined;
EDITOR = undefined;
$(function () {
	var getKey = function(key) {
      if (key == null) ctrl = event.ctrlKey;
      else ctrl = key.ctrlKey;
      if (key == null) keycode = event.keyCode;
      else keycode = key.keyCode;
      var lower = String.fromCharCode(keycode).toLowerCase();
      if (ctrl && lower == "b") $("#runbutton").click();
    }
    EDITOR = ace.edit("editor");
	//editor.setTheme("ace/theme/twilight");
	EDITOR.getSession().setMode("ace/mode/python");
	JQCONSOLE = $('#console').jqconsole('', '', '');
	var send = function (code) {
		if (WEB_SOCKET !== undefined) WEB_SOCKET.send(code);
		else JQCONSOLE.Write("Not connected to any server\n\n", 'jqconsole-output');
	}
	var startPrompt = function () {
		JQCONSOLE.Prompt(true, function (input) {
			send(input);
			startPrompt();
		});
	};
	startPrompt();
	$("#connectbutton").click(function() {
		var host = $("#connecthost").val();
		var port = $("#connectport").val();
		JQCONSOLE.Write("Connecting to server\n\n", 'jqconsole-output');
		WEB_SOCKET = new WebSocket("ws://"+host+":"+port);
		WEB_SOCKET.onopen = function() {
			JQCONSOLE.Write("Connection established\n\n", 'jqconsole-output');
        };
		WEB_SOCKET.onmessage = function(e) { 
			var line_limit = 500;
			var lines = e.data.replace('\r', '').split(/\n/);
			console.log(lines);
			for (var line in lines) {
				if (lines[line].length > line_limit)
					JQCONSOLE.Write(lines[line].substring(0, line_limit) + ' (line too long)', 'jqconsole-output'); 	
				JQCONSOLE.Write(lines[line], 'jqconsole-output');
				if (lines.length > 1 && line < lines.length - 1)
					JQCONSOLE.Write('\n', 'jqconsole-output');
			}
			

		};
		WEB_SOCKET.onclose = function() { WEB_SOCKET = undefined; JQCONSOLE.Write("Connection closed\n\n", 'jqconsole-output'); };
		WEB_SOCKET.onerror = function() { WEB_SOCKET = undefined; JQCONSOLE.Write("A connectivity error occurred\n\n", 'jqconsole-output'); };
	});
	$("#runbutton").click(function() {
		var ct = EDITOR.getCopyText();
		if (ct.length > 0) var code = ct;
		else {
			var currline = EDITOR.getSelectionRange().start.row;
			var code = EDITOR.session.getLine(currline);
			EDITOR.gotoLine(currline + 2, 0);
		}
		JQCONSOLE.Write(code.replace('\r', '')+'\n');
		send(code);
	});
	$(document).keyup(function(eventObj){
		getKey(eventObj);
	});
});