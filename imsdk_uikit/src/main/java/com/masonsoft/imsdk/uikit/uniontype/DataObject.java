package com.masonsoft.imsdk.uikit.uniontype;

import java.util.HashMap;
import java.util.Map;

public class DataObject<T> {

    public static final String EXT_KEY_TEXT_1 = "ext_key_text_1";
    public static final String EXT_KEY_TEXT_2 = "ext_key_text_2";
    public static final String EXT_KEY_OBJECT_1 = "ext_key_object_1";
    public static final String EXT_KEY_OBJECT_2 = "ext_key_object_2";
    public static final String EXT_KEY_BOOLEAN_1 = "ext_key_boolean_1";
    public static final String EXT_KEY_BOOLEAN_2 = "ext_key_boolean_2";
    public static final String EXT_KEY_HOLDER_ITEM_CLICK_1 = "ext_key_holder_item_click_1";
    public static final String EXT_KEY_HOLDER_ITEM_CLICK_2 = "ext_key_holder_item_click_2";
    public static final String EXT_KEY_HOLDER_ITEM_LONG_CLICK_1 = "ext_key_holder_item_long_click_1";
    public static final String EXT_KEY_HOLDER_ITEM_LONG_CLICK_2 = "ext_key_holder_item_long_click_2";

    public final T object;
    private final Map<String, Object> mExtObjects = new HashMap<>();

    public DataObject(T object) {
        this.object = object;
    }

    public DataObject<T> putExtObject(String extKey, Object extObject) {
        this.mExtObjects.put(extKey, extObject);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <E> E getExtObject(String extKey, E valueIfKeyNotFound) {
        E value = (E) this.mExtObjects.get(extKey);
        if (value != null || this.mExtObjects.containsKey(extKey)) {
            return value;
        }

        return valueIfKeyNotFound;
    }

    public DataObject<T> putExtObjectText1(String text) {
        return putExtObject(EXT_KEY_TEXT_1, text);
    }

    public String getExtObjectText1(String valueIfKeyNotFound) {
        return getExtObject(EXT_KEY_TEXT_1, valueIfKeyNotFound);
    }

    public DataObject<T> putExtObjectText2(String text) {
        return putExtObject(EXT_KEY_TEXT_2, text);
    }

    public String getExtObjectText2(String valueIfKeyNotFound) {
        return getExtObject(EXT_KEY_TEXT_2, valueIfKeyNotFound);
    }

    public DataObject<T> putExtObjectObject1(Object object) {
        return putExtObject(EXT_KEY_OBJECT_1, object);
    }

    public <E> E getExtObjectObject1(E valueIfKeyNotFound) {
        return getExtObject(EXT_KEY_OBJECT_1, valueIfKeyNotFound);
    }

    public DataObject<T> putExtObjectObject2(Object object) {
        return putExtObject(EXT_KEY_OBJECT_2, object);
    }

    public <E> E getExtObjectObject2(E valueIfKeyNotFound) {
        return getExtObject(EXT_KEY_OBJECT_2, valueIfKeyNotFound);
    }

    public DataObject<T> putExtObjectBoolean1(boolean bool) {
        return putExtObject(EXT_KEY_BOOLEAN_1, bool);
    }

    public boolean getExtObjectBoolean1(boolean valueIfKeyNotFound) {
        return getExtObject(EXT_KEY_BOOLEAN_1, valueIfKeyNotFound);
    }

    public DataObject<T> putExtObjectBoolean2(boolean bool) {
        return putExtObject(EXT_KEY_BOOLEAN_2, bool);
    }

    public boolean getExtObjectBoolean2(boolean valueIfKeyNotFound) {
        return getExtObject(EXT_KEY_BOOLEAN_2, valueIfKeyNotFound);
    }

    public DataObject<T> putExtHolderItemClick1(UnionTypeViewHolderListeners.OnItemClickListener listener) {
        return putExtObject(EXT_KEY_HOLDER_ITEM_CLICK_1, listener);
    }

    public UnionTypeViewHolderListeners.OnItemClickListener getExtHolderItemClick1() {
        return getExtObject(EXT_KEY_HOLDER_ITEM_CLICK_1, null);
    }

    public DataObject<T> putExtHolderItemClick2(UnionTypeViewHolderListeners.OnItemClickListener listener) {
        return putExtObject(EXT_KEY_HOLDER_ITEM_CLICK_2, listener);
    }

    public UnionTypeViewHolderListeners.OnItemClickListener getExtHolderItemClick2() {
        return getExtObject(EXT_KEY_HOLDER_ITEM_CLICK_2, null);
    }

    public DataObject<T> putExtHolderItemLongClick1(UnionTypeViewHolderListeners.OnItemLongClickListener listener) {
        return putExtObject(EXT_KEY_HOLDER_ITEM_LONG_CLICK_1, listener);
    }

    public UnionTypeViewHolderListeners.OnItemLongClickListener getExtHolderItemLongClick1() {
        return getExtObject(EXT_KEY_HOLDER_ITEM_LONG_CLICK_1, null);
    }

    public DataObject<T> putExtHolderItemLongClick2(UnionTypeViewHolderListeners.OnItemLongClickListener listener) {
        return putExtObject(EXT_KEY_HOLDER_ITEM_LONG_CLICK_2, listener);
    }

    public UnionTypeViewHolderListeners.OnItemLongClickListener getExtHolderItemLongClick2() {
        return getExtObject(EXT_KEY_HOLDER_ITEM_LONG_CLICK_2, null);
    }

}
