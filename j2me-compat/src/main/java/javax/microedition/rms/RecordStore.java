package javax.microedition.rms;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import javax.microedition.lcdui.CanvasView;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * J2ME RecordStore compatibility layer.
 * Uses Android SharedPreferences as backing store.
 * Records are stored as Base64-encoded strings.
 */
public class RecordStore {
    private static Map<String, RecordStore> openStores = new HashMap<>();
    
    private String name;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean open = false;
    private int nextRecordId;
    
    private RecordStore(String name, SharedPreferences prefs) {
        this.name = name;
        this.prefs = prefs;
        this.nextRecordId = prefs.getInt("__nextId", 1);
        this.open = true;
    }
    
    /**
     * Open (or create) a record store.
     */
    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) 
            throws RecordStoreException, RecordStoreNotFoundException {
        
        // Check if already open
        if (openStores.containsKey(recordStoreName)) {
            return openStores.get(recordStoreName);
        }
        
        Context context = CanvasView.getAppContext();
        if (context == null) {
            throw new RecordStoreException("Context not initialized");
        }
        
        SharedPreferences prefs = context.getSharedPreferences(
            "J2ME_RMS_" + recordStoreName, Context.MODE_PRIVATE);
        
        // Check if store exists
        boolean exists = prefs.contains("__created");
        
        if (!exists && !createIfNecessary) {
            throw new RecordStoreNotFoundException("Record store not found: " + recordStoreName);
        }
        
        if (!exists) {
            // Initialize new store
            prefs.edit()
                .putBoolean("__created", true)
                .putInt("__nextId", 1)
                .apply();
        }
        
        RecordStore store = new RecordStore(recordStoreName, prefs);
        openStores.put(recordStoreName, store);
        return store;
    }
    
    /**
     * Delete a record store.
     */
    public static void deleteRecordStore(String recordStoreName)
            throws RecordStoreException, RecordStoreNotFoundException {
        
        Context context = CanvasView.getAppContext();
        if (context == null) {
            throw new RecordStoreException("Context not initialized");
        }
        
        // Close if open
        if (openStores.containsKey(recordStoreName)) {
            openStores.get(recordStoreName).closeRecordStore();
        }
        
        SharedPreferences prefs = context.getSharedPreferences(
            "J2ME_RMS_" + recordStoreName, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
    
    /**
     * List available record stores.
     */
    public static String[] listRecordStores() {
        // Android doesn't provide easy enumeration of SharedPreferences
        // This would need to be tracked separately
        return new String[0];
    }
    
    /**
     * Close the record store.
     */
    public void closeRecordStore() throws RecordStoreException {
        if (!open) {
            throw new RecordStoreNotOpenException("Store already closed");
        }
        
        // Commit any pending changes
        if (editor != null) {
            editor.apply();
            editor = null;
        }
        
        open = false;
        openStores.remove(name);
    }
    
    /**
     * Get number of records.
     */
    public int getNumRecords() throws RecordStoreNotOpenException {
        checkOpen();
        
        int count = 0;
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            if (!key.startsWith("__") && key.startsWith("record_")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get size of a record.
     */
    public int getRecordSize(int recordId) 
            throws RecordStoreNotOpenException, InvalidRecordIDException {
        checkOpen();
        
        String key = "record_" + recordId;
        if (!prefs.contains(key)) {
            throw new InvalidRecordIDException("Record not found: " + recordId);
        }
        
        String encoded = prefs.getString(key, "");
        byte[] data = Base64.decode(encoded, Base64.DEFAULT);
        return data.length;
    }
    
    /**
     * Add a new record.
     */
    public int addRecord(byte[] data, int offset, int numBytes) 
            throws RecordStoreException, RecordStoreNotOpenException {
        checkOpen();
        
        int recordId = nextRecordId++;
        
        // Copy the relevant portion
        byte[] recordData = new byte[numBytes];
        System.arraycopy(data, offset, recordData, 0, numBytes);
        
        // Store as Base64
        String encoded = Base64.encodeToString(recordData, Base64.DEFAULT);
        
        prefs.edit()
            .putString("record_" + recordId, encoded)
            .putInt("__nextId", nextRecordId)
            .apply();
        
        return recordId;
    }
    
    /**
     * Delete a record.
     */
    public void deleteRecord(int recordId) 
            throws RecordStoreNotOpenException, InvalidRecordIDException {
        checkOpen();
        
        String key = "record_" + recordId;
        if (!prefs.contains(key)) {
            throw new InvalidRecordIDException("Record not found: " + recordId);
        }
        
        prefs.edit().remove(key).apply();
    }
    
    /**
     * Set/update a record.
     */
    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreException, RecordStoreNotOpenException, InvalidRecordIDException {
        checkOpen();
        
        String key = "record_" + recordId;
        if (!prefs.contains(key)) {
            throw new InvalidRecordIDException("Record not found: " + recordId);
        }
        
        // Copy the relevant portion
        byte[] recordData = new byte[numBytes];
        System.arraycopy(newData, offset, recordData, 0, numBytes);
        
        // Store as Base64
        String encoded = Base64.encodeToString(recordData, Base64.DEFAULT);
        prefs.edit().putString(key, encoded).apply();
    }
    
    /**
     * Get a record.
     */
    public byte[] getRecord(int recordId) 
            throws RecordStoreNotOpenException, InvalidRecordIDException {
        checkOpen();
        
        String key = "record_" + recordId;
        if (!prefs.contains(key)) {
            throw new InvalidRecordIDException("Record not found: " + recordId);
        }
        
        String encoded = prefs.getString(key, "");
        return Base64.decode(encoded, Base64.DEFAULT);
    }
    
    /**
     * Get a record into an existing buffer.
     */
    public int getRecord(int recordId, byte[] buffer, int offset)
            throws RecordStoreNotOpenException, InvalidRecordIDException {
        checkOpen();
        
        byte[] data = getRecord(recordId);
        System.arraycopy(data, 0, buffer, offset, data.length);
        return data.length;
    }
    
    /**
     * Get the name of this store.
     */
    public String getName() throws RecordStoreNotOpenException {
        checkOpen();
        return name;
    }
    
    /**
     * Get available space.
     */
    public int getSizeAvailable() throws RecordStoreNotOpenException {
        checkOpen();
        return Integer.MAX_VALUE; // Effectively unlimited on modern devices
    }
    
    /**
     * Get total size used.
     */
    public int getSize() throws RecordStoreNotOpenException {
        checkOpen();
        // Approximate - would need to sum all records
        return 0;
    }
    
    /**
     * Get last modified time.
     */
    public long getLastModified() throws RecordStoreNotOpenException {
        checkOpen();
        return System.currentTimeMillis();
    }
    
    /**
     * Get version number.
     */
    public int getVersion() throws RecordStoreNotOpenException {
        checkOpen();
        return prefs.getInt("__version", 0);
    }
    
    /**
     * Get next available record ID.
     */
    public int getNextRecordID() throws RecordStoreException, RecordStoreNotOpenException {
        checkOpen();
        return nextRecordId;
    }
    
    private void checkOpen() throws RecordStoreNotOpenException {
        if (!open) {
            throw new RecordStoreNotOpenException("Record store is not open");
        }
    }
}
