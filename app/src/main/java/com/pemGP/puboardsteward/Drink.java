package com.pemGP.puboardsteward;

import java.io.Serializable;

/**
 * Created by Florian on 01.08.2014.
 */

public class Drink implements Serializable
{
    /** Properties of drink**/

    private double mPrice;
    private String mName;
    private int mId;

    /** Constructor of drink **/

    public Drink(String nameString,double value, int id)
    {
        mName = nameString;
        mPrice = value;
        mId = id;
    }

    public Drink()
    {
       this("", 0, 0);
    }

    // Getters and Setters

    public String getName()
    {
        return mName;
    }

    public double getPrice()
    {
        return mPrice;
    }

    public Integer getId() {
        return mId;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public void setPrice(double price) {
        this.mPrice = price;
    }

    @Override
    public String toString()
    {
        return mName+"\n£"+ mPrice +"0";
    }

    public String toString(String option){
        if (option.equalsIgnoreCase("history"))
            return " £"+ mPrice +"0"+"\t"+mName;
        else
            return "";
    }

}