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
package org.vsepml.storm.twitter;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import twitter4j.GeoLocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 10/01/15.
 */
public class StormTwitterHashTagIdentifier extends BaseRichBolt {

    private static final Logger journal = Logger.getLogger(StormTwitterStreamSpout.class.getName());

    private OutputCollector collector;
    private ArrayList<String> identifiers = new ArrayList<String>();

    public void addIdentifier(String filter){
        identifiers.add(filter.toLowerCase());
    }

    public void removeIdentifier(String filter){
        identifiers.remove(filter.toLowerCase());
    }

    public StormTwitterHashTagIdentifier(ArrayList<String> identifiers){
        super();
        this.identifiers=identifiers;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        String hashtag = (String) tuple.getValueByField("hashtag");
        if(filterHashTag(hashtag.toLowerCase())){
            if(tuple.getValueByField("geolocation") != null) {
                GeoLocation l = (GeoLocation) tuple.getValueByField("geolocation");
                collector.emit(new Values(l.getLatitude() + "," + l.getLongitude()));
            }
        }
    }

    private Boolean filterHashTag(String hashTag){
        for(String identifier: identifiers){
            if (hashTag.toLowerCase().contains(identifier)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("latlong"));
    }
}
