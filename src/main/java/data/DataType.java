/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.Serializable;

/**
 *
 * @author Peter
 */
public interface DataType<V extends DataType<V>> extends HasValue<V>, Serializable, Cloneable {

    public abstract void set(V other);

    public abstract void add(V other);

    public abstract void subtract(V other);

    public abstract void reset();

    public abstract V cloneThis();

    public abstract V cloneNew();

    @Override
    public abstract String toString();

    public abstract String toString(String format);
}
