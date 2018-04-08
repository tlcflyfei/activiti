
import org.activiti.engine.ProcessEngine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 注意 坑：排他网关有个默认的选项  default flow ,
 *  当 default flow 设定后  就不用设置表达式了,
 *  如果所有的条件都不通过 就会执行默认的流程
 * @author xuecheng
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:applicationContext.xml"})
public class ActivitiJunitTest {
    private static Logger logger = LoggerFactory.getLogger(ActivitiJunitTest.class);
	@Autowired
    private ProcessEngine processEngine;//流程引擎对象


	@Test
	public void start(){
        logger.info("============processEngine:"+processEngine+"Create Success!!");
	}



}
