WEB_SOCKET_SWF_LOCATION = "WebSocketMain.swf";
WEB_SOCKET_DEBUG = true;
WEB_SOCKET = undefined;
JQCONSOLE = undefined;
EDITOR = undefined;
$(function () {
	EDITOR = ace.edit("editor");
	//editor.setTheme("ace/theme/twilight");
	EDITOR.getSession().setMode("ace/mode/python");
	JQCONSOLE = $('#console').jqconsole('', '', '');
	var send = function (code) {
		if (WEB_SOCKET !== undefined) WEB_SOCKET.send(code) 
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
		JQCONSOLE.Write("Connecting to server\n\n", 'jqconsole-output');
		WEB_SOCKET = new WebSocket("ws://"+$("#connecthost").val()+":"+$("#connectport").val());
		WEB_SOCKET.onopen = function() { JQCONSOLE.Write("Connection established\n\n", 'jqconsole-output'); };
		WEB_SOCKET.onmessage = function(e) { JQCONSOLE.Write(e.data.replace('\r', ''), 'jqconsole-output'); };
		WEB_SOCKET.onclose = function() { WEB_SOCKET = undefined; JQCONSOLE.Write("Connection closed\n\n", 'jqconsole-output'); };
		WEB_SOCKET.onerror = function() { WEB_SOCKET = undefined; JQCONSOLE.Write("A connectivity error occurred\n\n", 'jqconsole-output'); };
	});
	$("#runbutton").click(function() { 
		var ct = EDITOR.getCopyText();
		if (ct.length > 0)
		send(ct);
		else
		send(EDITOR.getValue());
	});
});