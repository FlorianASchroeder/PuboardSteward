package com.pemGP.puboardsteward;

import java.io.Serializable;

/**
 * Created by Florian on 01.08.2014.
 */
public class mAccount implements Serializable
{
    private String accName;
    private String accID;
    private double accMoney;

    public mAccount(String n, String id, double m){
        accName = n;
        accID = id;
        accMoney = m;
    }

    public mAccount(String n, String id){
        this(n,id,0);
    }

    public mAccount() {
        this("","",0);
    }

    public void setAccID(String accID)
    {
        this.accID = accID;
    }

    public String getAccID()
    {
        return accID;
    }

    public void setAccName(String accName)
    {
        this.accName = accName;
    }

    public String getAccName()
    {
        return accName;
    }

    public void setAccMoney(double accMoney)
    {
        this.accMoney = accMoney;
    }

    public double getAccMoney()
    {
        return accMoney;
    }

    public int addAccMoney(double accMoney)
    {
        if ((this.accMoney += accMoney) < 0) {
            // needs top up
            return -1;
        }
        return 0;

    }

    @Override
    public String toString() {
        return accName;
    }

    /*  No need for implementation, as all Members are Serializable!
        private void writeObject(ObjectOutputStream out) throws IOException {
            //needed for serialization
            out.writeBytes(accName+"\n");
            out.writeBytes(accID+"\n");
            out.writeDouble(accMoney);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            // needed for serialization
            // TODO: replace with better
            accName = in.readLine();
            accID = in.readLine();
            accMoney = in.readDouble();
        }

        private void readObjectNoData(){
            accMoney=0;
            accName="";
            accID="";
        }
        */
}
