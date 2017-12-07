package com.yijiupi.himalaya;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hello world!
 *
 */
public class App {
	public static class PingCounter extends BaseRichBolt {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private OutputCollector collector;

		@Override
		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			System.out.println("++++++++++++++++++++declareOutputFields+++++++++++++++++++++");
		}

		@Override
		public void execute(Tuple input) {
			String msg = input.getString(0);
			ObjectMapper mapper = new ObjectMapper();
			try {
				RowMap rowMap = mapper.readValue(msg, RowMap.class);
				System.out.println("type:" + rowMap.getType());
				System.out.println("---------------------" + rowMap.toString() + "-----------------");
				collector.ack(input);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			this.collector = arg2;
			System.out.println("++++++++++++++++++++prepare++++++++++++++++++++++++++++++++++");
		}

	}

	public static void main(String[] args) {
		String zkhost = "172.16.1.57:2181,172.16.1.58:2181,172.16.1.59:2181";
		String topic = "maxwell";
		String groupId = "trading-group";
		int spoutNum = 1;
		int boltNum = 1;
		ZkHosts zkHosts = new ZkHosts(zkhost);// kafaka所在的zookeeper
		SpoutConfig spoutConfig = new SpoutConfig(zkHosts, topic, "/kafka", groupId);
		spoutConfig.zkPort = 2181;
		spoutConfig.zkServers = Arrays.asList("172.16.1.57", "172.16.1.58", "172.16.1.59", "172.16.1.78");

		spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
		KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);

		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("kafka-reader", kafkaSpout, 3);
		builder.setBolt("ping-counter", new PingCounter(), 3).shuffleGrouping("kafka-reader");
		Config conf = new Config();
		conf.setDebug(true);
		// 设置任务线程数
		conf.setMaxTaskParallelism(1);

		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("test", conf, builder.createTopology());
	}
}
