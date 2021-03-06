package org.opendedup.sdfs.mgmt.cli;

import java.net.URLEncoder;
import java.util.Formatter;

import org.opendedup.logging.SDFSLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProcessDeleteFileCmd {
	public String status;
	public String msg;

	public static ProcessDeleteFileCmd execute(String file,boolean rmlock) {
		ProcessDeleteFileCmd store = new ProcessDeleteFileCmd();
		try {
			file = URLEncoder.encode(file, "UTF-8");
			StringBuilder sb = new StringBuilder();
			Formatter formatter = new Formatter(sb);
			SDFSLogger.getLog().debug("Deleting File [" + file + "] ");
			formatter.format("file=%s&cmd=%s&options=%s&retentionlock=%s", file, "deletefile",
					"",Boolean.toString(rmlock));
			Document doc = MgmtServerConnection.getResponse(sb.toString());
			formatter.close();
			Element root = doc.getDocumentElement();
			store.status = root.getAttribute("status");
			store.msg = root.getAttribute("msg");

		} catch (Exception e) {
			store.status = "failed";
			store.msg = e.getMessage();
		}
		return store;
	}

}
