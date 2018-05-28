package org.opendedup.sdfs.mgmt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendedup.collections.LongByteArrayMap;
import org.opendedup.collections.LongKeyValue;
import org.opendedup.collections.SparseDataChunk;
import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.MetaFileStore;
import org.opendedup.sdfs.io.HashLocPair;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.sdfs.notification.SDFSEvent;
import org.opendedup.util.StringUtils;
import org.w3c.dom.Element;

import com.google.common.primitives.Longs;

public class OutputDDBContents implements Runnable {

	MetaDataDedupFile mf = null;
	String sfile, dstfile;
	private static transient ThreadPoolExecutor executor = new ThreadPoolExecutor(1,
			Main.writeThreads + 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(Main.writeThreads));
	static {
		executor.allowCoreThreadTimeOut(true);
	}
	SDFSEvent fevt = null;
	public Element getResult(String file, String dstfile) throws IOException {

		File f = new File(Main.volume.getPath() + File.separator + file);

			if(!f.exists()) {
				throw new IOException("file ["+file+"] does not exist");
			}
			this.mf = MetaFileStore.getMF(f.getPath());
			this.sfile = f.getPath();
			this.dstfile = new File(dstfile).getPath();
			if(!new File(this.dstfile).getParentFile().exists()) {
				new File(this.dstfile).getParentFile().mkdirs();
			}
			fevt = SDFSEvent.cfEvent(file);
			try {
				Thread th = new Thread(this);
				executor.execute(th);
				return fevt.toXML();
			} catch (Exception e) {
				throw new IOException(e);
			}
	}


	private void checkDedupFile() throws IOException {

		SDFSLogger.getLog().info("Debug output for " + mf.getDfGuid() + " to " + this.dstfile);
		LongByteArrayMap ddb = LongByteArrayMap.getMap(mf.getDfGuid(), mf.getLookupFilter());
		File fout = new File(this.dstfile);
		FileWriter fileWriter = new FileWriter(fout);
		PrintWriter printWriter = new PrintWriter(fileWriter);
		try {
			long fp = 0;
			ddb.iterInit();
			for (;;) {
				LongKeyValue kv = ddb.nextKeyValue(false);
				if (kv == null)
					break;
				SparseDataChunk ck = kv.getValue();
				TreeMap<Integer, HashLocPair> al = ck.getFingers();
				for (HashLocPair p : al.values()) {
					printWriter.write(fp + "," + p.nlen + "," + StringUtils.getHexString(p.hash) + "," + Longs.fromByteArray(p.hashloc)+"\r\n");
					fp += p.nlen;
				}
			}
			printWriter.close();
		} catch (Throwable e) {
			SDFSLogger.getLog().warn("error while checking file [" + ddb + "]", e);
			throw new IOException(e);
		} finally {
			try {
				ddb.close();
				fevt.endEvent();
			} catch (Exception e) {
				SDFSLogger.getLog().warn("error closing file [" + mf.getPath() + "]", e);
			}

		}
		SDFSLogger.getLog().info("Done outputing " + mf.getDfGuid() + " to " + this.dstfile);
	}

	@Override
	public void run() {
		try {
			this.checkDedupFile();
		} catch (Exception e) {
			SDFSLogger.getLog().error("unable to process file " + this.sfile, e);
		}

	}

}
