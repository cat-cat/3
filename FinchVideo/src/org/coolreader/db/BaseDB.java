package org.coolreader.db;

import java.io.File;
import org.coolreader.crengine.Utils;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public abstract class BaseDB {
	
	protected SQLiteDatabase mDB;
	private File mFileName;
	private boolean restoredFromBackup;
	private boolean error = false;

	public File getFileName() {
		return mFileName;
	}

	public boolean isOpened() {
		if (mDB != null && !error)
			return true;
		Log.w("MyTrace", "CoolReader: " + "DB access while not opened");
		return false;
	}

	public boolean open(File dir) {
		error = false;
		File dbFile = new File(dir, dbFileName());
		Log.i("MyTrace", "CoolReader: " + "opening DB " + dbFile);
		mFileName = dbFile;
		mDB = openDB(dbFile);
		if (mDB == null) {
			return false;
		}
		boolean res = checkSchema();
		if (!res) {
			Log.e("MyTrace", "CoolReader: " + "Closing DB due error while upgrade of schema: " + dbFile.getAbsolutePath());
			close();
			Utils.moveCorruptedFileToBackup(dbFile);
			if (!restoredFromBackup)
				Utils.restoreFromBackup(dbFile);
			mDB = openDB(dbFile);
			res = checkSchema();
			if (!res)
				close();
		}
		if (mDB != null) {
			return true;
		}
		return false;
	}

	public boolean close() {
		if (mDB != null) {
			try {
				Log.i("MyTrace", "CoolReader: " + "Closing database");
				flush();
				clearCaches();
				mDB.close();
				mDB = null;
				return true;
			} catch (SQLiteException e) {
				Log.e("MyTrace", "CoolReader: " + "Error while closing DB " + mFileName);
			}
			mDB = null;
		}
		return false;
	}

	protected boolean checkSchema() {
		try {
			upgradeSchema();
			return true;
		} catch (SQLiteException e) {
			return false;
		}
	}

	protected abstract boolean upgradeSchema();

	protected abstract String dbFileName();
	
	private SQLiteDatabase openDB(File dbFile) {
		restoredFromBackup = false;
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			return db;
		} catch (SQLiteException e) {
			Log.e("MyTrace", "CoolReader: " + "Error while opening DB " + dbFile.getAbsolutePath());
			Utils.moveCorruptedFileToBackup(dbFile);
			restoredFromBackup = Utils.restoreFromBackup(dbFile);
			try {
				db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
				return db;
			} catch (SQLiteException ee) {
				Log.e("MyTrace", "CoolReader: " + "Error while opening DB " + dbFile.getAbsolutePath());
			}
		}
		return null;
	}

	public void execSQLIgnoreErrors( String... sqls )
	{
		for ( String sql : sqls ) {
			try { 
				mDB.execSQL(sql);
			} catch ( SQLException e ) {
				// ignore
				Log.w("cr3db", "query failed, ignoring: " + sql);
			}
		}
	}
	
	protected void ensureOpened() {
		if (!isOpened())
			throw new DBRuntimeException("DB is not opened");
	}

	public void execSQL( String... sqls )
	{
		ensureOpened();
		for ( String sql : sqls ) {
			try { 
				mDB.execSQL(sql);
			} catch ( SQLException e ) {
				// ignore
				Log.w("cr3", "query failed: " + sql);
				throw e;
			}
		}
	}

	public Long longQuery( String sql )
	{
		ensureOpened();
		SQLiteStatement stmt = null;
		try {
			stmt = mDB.compileStatement(sql);
			return stmt.simpleQueryForLong();
		} catch ( Exception e ) {
			// not found or error
			return null;
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}
	
	public String stringQuery( String sql )
	{
		ensureOpened();
		SQLiteStatement stmt = null;
		try {
			stmt = mDB.compileStatement(sql);
			return stmt.simpleQueryForString();
		} catch ( Exception e ) {
			// not found or error
			return null;
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}
	
	public static String quoteSqlString(String src) {
		if (src==null)
			return "null";
		String s = src.replaceAll("\\'", "\\\\'");
		return "'" + s + "'";
	}
	
	public void clearCaches() {
		// override it
	}

	private boolean changed = false;
	
	/**
	 * Begin transaction, if not yet started, for changes.
	 */
	public void beginChanges() {
		if (!mDB.inTransaction()) {
			Log.v("MyTrace", "CoolReader: " + "starting writable transaction");
			mDB.beginTransaction();
		}
		if (!changed) {
			Log.v("MyTrace", "CoolReader: " + "modify readonly transaction to writable");
			changed = true;
		}
	}

	/**
	 * Begin transaction, if not yet started, for faster reading.
	 */
	public void beginReading() {
		if (!mDB.inTransaction()) {
			Log.v("MyTrace", "CoolReader: " + "starting readonly transaction");
			mDB.beginTransaction();
		}
	}

	/**
	 * Rolls back transaction, if writing is not started.
	 */
	public void endReading() {
		if (mDB.inTransaction() && !changed) {
			Log.v("MyTrace", "CoolReader: " + "ending readonly transaction");
			mDB.endTransaction();
		}
	}

	/**
	 * Commits or rolls back transaction, if started, and frees DB resources.
	 * Will commit only if beginChanges() has been called. Otherwise will roll back.
	 */
	public void flush() {
		if (mDB != null && mDB.inTransaction()) {
			if (changed) {
				changed = false;
				mDB.setTransactionSuccessful();
				Log.i("MyTrace", "CoolReader: " + "flush: committing changes");
			} else {
				Log.i("MyTrace", "CoolReader: " + "flush: rolling back changes");
			}
			mDB.endTransaction();
		}
	}
}
