/**
 * Copyright 2015 Nicolas Ferry <${email}>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vsepml.storm.mcsuite;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 09/03/2016.
 */
public class CSVLineSplitterBolt extends BaseRichBolt {
    private final char separator;
    private OutputCollector collector;
    private CSVReader reader;
    private String[] headers;
    private AtomicLong a = new AtomicLong(0);
    private ArrayList<Header> al=new ArrayList<Header>();

    private static final Logger journal = Logger.getLogger(CSVLineSplitterBolt.class.getName());

    public CSVLineSplitterBolt(char separator) {
        this.separator = separator;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        reader = new CSVReader(new StringReader(tuple.getStringByField("measurement")), separator);
        String[] nextLine;
        try {
            nextLine = reader.readNext();
            if (nextLine != null){
                if ((!nextLine[0].equals("Date")) && (!nextLine[0].equals("Fecha"))) {
                    long epoch=0;
                    if(al.get(0).getName().equals("Date")) {
                        Instant fromIso8601 = Instant.parse(nextLine[0]);
                        epoch=fromIso8601.toEpochMilli();//convert from iso8601 to unix epoch
                    }else {
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        epoch=parser.parse(nextLine[0]).toInstant().toEpochMilli();
                    }
                    for (int i = 1; i < headers.length; i++) {
                        Long tmp=a.incrementAndGet();
                        Header h=al.get(i-1);
                        collector.emit(new Values(h.getName(), nextLine[i], tmp, epoch, h.getUnit(), h.getType(),h.getCoeff())); //First should always be the timestamp
                    }
                } else {
                    headers = nextLine;
                    for (int i = 1; i < headers.length; i++) {
                        al.add(new Header(headers[i]));
                    }
                }
            }
            /*while ((nextLine = reader.readNext()) != null) {
                Long tmp=al.incrementAndGet();
                for(int i=0; i < headers.length;i++){
                    System.out.println(headers[i] + "" + nextLine[i] + "" + tmp);
                    collector.emit(new Values(headers[i], nextLine[i], tmp));
                }
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            collector.ack(tuple);
        }
    }

    public boolean isDouble( String str ){
        try {
            Double.parseDouble(str);
            return true;
        }catch( Exception e ){
            return false;
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("SensorId","MeasurementsAtTimeT", "MeasurementId", "Timestamp", "unit", "type", "coeff"));
    }
}

