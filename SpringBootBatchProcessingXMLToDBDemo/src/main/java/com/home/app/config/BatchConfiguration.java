package com.home.app.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.xstream.XStreamMarshaller;

import com.home.app.model.Person;
import com.home.app.model.PersonRowMapper;
import com.home.app.processor.PersonItemProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;

	@Bean
	public StaxEventItemReader<Person> reader() {
		StaxEventItemReader<Person> itemreader=new StaxEventItemReader<>();
		itemreader.setResource(new ClassPathResource("persons.xml"));
		itemreader.setFragmentRootElementName("person");
		
		Map<String,String> map=new HashMap<String,String>();
		map.put("person", "com.home.app.model.Person");
		XStreamMarshaller marshaller=new XStreamMarshaller();
		marshaller.setAliases(map);
		itemreader.setUnmarshaller(marshaller);
		return itemreader;
	}

	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Person> writer(){
		JdbcBatchItemWriter<Person> itemWriter=new JdbcBatchItemWriter<>();
		itemWriter.setDataSource(dataSource);
		itemWriter.setSql("INSERT INTO person (person_id,first_name, last_name,email,age) VALUES (?,?,?,?,?)");
		itemWriter.setItemPreparedStatementSetter(new PersonPreparedStatementSetter());
		return itemWriter;
		
	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").<Person, Person>chunk(100).reader(reader()).processor(processor())
				.writer(writer()).build();
	}

	@Bean
	public Job exportPersonJob() {
		return jobBuilderFactory.get("exportPersonJob").incrementer(new RunIdIncrementer()).flow(step1()).end().build();
	}

}