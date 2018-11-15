/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

/**
 *
 * @author Peter
 */
public interface HasValue<V extends DataType<V>> {
    public abstract V getValue();
}
