package cz.vsb.cbe.testbed.sql;

import java.util.Date;

public class Record{
    private int DeId;
    private float DaValue;
    private Date DaTimeStamp;

    public Record(int deId, float daValue, long daTimeStamp){
        DeId = deId;
        DaValue = daValue;
        DaTimeStamp = new Date(daTimeStamp);
    }

    public Record(Record record){
        DeId = record.getId();
        DaValue = record.getValue();
        DaTimeStamp = record.getTimeStamp();
    }

    public int getId(){
        return DeId;
    }

    public void setValue(float value){
        DaValue = value;
    }

    public float getValue() {
        return DaValue;
    }

    public Date getTimeStamp() {
        return DaTimeStamp;
    }

}
