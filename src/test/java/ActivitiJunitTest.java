
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;

import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * 与流程定义和部署对象相关的Service

	 */
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	ProcessEngineConfiguration processEngineConfiguration;
	@Test
	public void start(){
        logger.info("============processEngine:"+processEngine+"Create Success!!");
	}

	/**部署流程定义*/
	@Test
	public void deploymentProcessDefinition(){

		DeploymentBuilder deploymentBuilder=repositoryService.createDeployment();//创建一个部署对象
		deploymentBuilder.name("审批流程demo1");//添加部署的名称
		deploymentBuilder.addClasspathResource("activiti/process.bpmn");//从classpath的资源加载，一次只能加载一个文件
		deploymentBuilder.addClasspathResource("activiti/process.png");//从classpath的资源加载，一次只能加载一个文件

		Deployment deployment=deploymentBuilder.deploy();//完成部署

		//打印我们的流程信息
		System.out.println("流程Id:"+deployment.getId());
		System.out.println("流程Name:"+deployment.getName());
	}

	/**启动流程引擎*/
	@Test
	public void startProcessInstance(){
		Map<String, Object> variables = new HashMap<>();
		variables.put("employeeName", "wish1");
		variables.put("numberOfDays", new Integer(4));
		variables.put("vacationMotivation", "I'm really 111");
		processEngine.getIdentityService().setAuthenticatedUserId("张三");//调用官方的开放API；
		RuntimeService runtimeService = processEngine.getRuntimeService();
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_1", variables);


//		//使用流程定义的key，key对应bpmn文件对应的id，
//		//(也是act_re_procdef表中对应的KEY_字段),默认是按照最新版本启动
//		String processDefinitionkey="myProcess_1";//流程定义的key就是HelloWorld
//		//获取流程实例对象
//		ProcessInstance processInstance=runtimeService.startProcessInstanceByKey(processDefinitionkey);
		System.out.println("流程实例ID："+processInstance.getId());//流程实例ID
		System.out.println("流程定义ID："+processInstance.getProcessDefinitionId());//流程定义ID
		System.out.println("activitiId::"+processInstance.getActivityId());
		System.out.println("任务ID："+processEngine.getTaskService().createTaskQuery().singleResult().getId());


	}


	/**获取任务*/
	@Test
	public void claimTaskListByUserId(){
		String taskId="45008";//上一次我们查询的任务ID就是304.
		processEngine.getTaskService()
				.claim(taskId, "");//个人任务的办理人
		System.out.println("任务领取结束");

	}


	/**查询当前的个人任务(实际就是查询act_ru_task表)*/
	@Test
	public void findMyPersonalTask(){
//		String assignee="张三";
		//获取事务Service
		TaskService taskService=processEngine.getTaskService();
		List<Task> taskList=taskService.createTaskQuery()//创建任务查询对象
				//.executionId("10001")
				//.taskAssignee(assignee)//指定个人任务查询，指定办理人
				.taskCandidateGroup("custom")
				.list();//获取该办理人下的事务列表

		if(taskList!=null&&taskList.size()>0){
			for(Task task:taskList){
				System.out.println("任务ID："+task.getId());
				System.out.println("任务名称："+task.getName());
				System.out.println("任务的创建时间："+task.getCreateTime());
				System.out.println("任务办理人："+task.getAssignee());
				System.out.println("流程实例ID："+task.getProcessInstanceId());
				System.out.println("执行对象ID："+task.getExecutionId());
				System.out.println("流程定义ID："+task.getProcessDefinitionId());
				System.out.println("描述："+task.getDescription());
				System.out.println("："+task.getCategory());


				System.out.println("#############################################");
			}
		}
	}



	/**完成我的任务*/
	@Test
	public void completeMyPersonalTask(String taskId,String userId){
		if(StringUtils.isEmpty(taskId)){
			taskId="30008";//上一次我们查询的任务ID就是304.
		}
		if(StringUtils.isEmpty(userId)){
			userId="张三";//上一次我们查询的任务ID就是304.
		}
		processEngine.getTaskService().setAssignee(taskId, userId);
		TaskService taskService=processEngine.getTaskService();
		taskService.complete(taskId);//完成taskId对应的任务
		System.out.println("完成ID为"+taskId+"的任务");

	}

	/**删除key相同的所有不同版本的流程定义*/
	@Test
	public void deleteProcessDefinitionByKey(){
		//流程定义的key
		String processDefinitionKey = "myProcess_1";
		//先使用流程定义的key查询流程定义，查询出所有的版本
		RepositoryService repositoryServic=processEngine.getRepositoryService();
		ProcessDefinitionQuery query=repositoryServic.createProcessDefinitionQuery();
		List<ProcessDefinition> list=query.processDefinitionKey(processDefinitionKey).list();

		//遍历，获取每个流程定义的部署ID
		if(list!=null&&list.size()>0){
			for(ProcessDefinition pd:list){
				//获取部署ID
				String deploymentId = pd.getDeploymentId();
				processEngine.getRepositoryService()
						.deleteDeployment(deploymentId,true);//级联删除
			}
		}
	}


	/**
	 * 高亮显示流程
	 */
	@Test
	public void currentTaskNodePng() throws Exception{
		//流程ID
		String processInstanceId = "25001";
		//activitiID
		String activityIds = "25001";
		//图实例
		//获取历史流程实例
		HistoricProcessInstance processInstance = processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		//获取流程图
		BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(processInstance.getProcessDefinitionId());
//		processEngineConfiguration = processEngine.getProcessEngineConfiguration();
//		Context.setProcessEngineConfiguration((ProcessEngineConfigurationImpl) processEngineConfiguration);

		ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
		ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

		List<HistoricActivityInstance> highLightedActivitList =  processEngine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
		//高亮环节id集合
		List<String> highLightedActivitis = new ArrayList<String>();
		//高亮线路id集合
		List<String> highLightedFlows = getHighLightedFlows(definitionEntity,highLightedActivitList);

		for(HistoricActivityInstance tempActivity : highLightedActivitList){
			String activityId = tempActivity.getActivityId();
			highLightedActivitis.add(activityId);
		}
//中文显示的是口口口，设置字体就好了
		InputStream inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitis,highLightedFlows,"宋体","宋体",null,1.0);
	  	File file = new File("C:\\Users\\Administrator\\Desktop\\1.png");
		OutputStream outputStream = new FileOutputStream(file);

		byte[] bytes = input2byte(inputStream);

		for(int i=0;i<bytes.length;i++){        // 采用循环方式写入
			outputStream.write(bytes[i]) ;    // 每次只写入一个内容
		}
		inputStream.close();
		outputStream.close();

	}


	public static final byte[] input2byte(InputStream inStream)
			throws IOException {
		ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
		byte[] buff = new byte[100];
		int rc = 0;
		while ((rc = inStream.read(buff, 0, 100)) > 0) {
			swapStream.write(buff, 0, rc);
		}
		byte[] in2b = swapStream.toByteArray();
		return in2b;
	}

	/**
	 * 获取需要高亮的线
	 * @param processDefinitionEntity
	 * @param historicActivityInstances
	 * @return
	 */
	private List<String> getHighLightedFlows(
			ProcessDefinitionEntity processDefinitionEntity,
			List<HistoricActivityInstance> historicActivityInstances) {
		List<String> highFlows = new ArrayList<String>();// 用以保存高亮的线flowId
		for (int i = 0; i < historicActivityInstances.size() - 1; i++) {// 对历史流程节点进行遍历
			ActivityImpl activityImpl = processDefinitionEntity
					.findActivity(historicActivityInstances.get(i)
							.getActivityId());// 得到节点定义的详细信息
			List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();// 用以保存后需开始时间相同的节点
			ActivityImpl sameActivityImpl1 = processDefinitionEntity
					.findActivity(historicActivityInstances.get(i + 1)
							.getActivityId());
			// 将后面第一个节点放在时间相同节点的集合里
			sameStartTimeNodes.add(sameActivityImpl1);
			for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
				HistoricActivityInstance activityImpl1 = historicActivityInstances
						.get(j);// 后续第一个节点
				HistoricActivityInstance activityImpl2 = historicActivityInstances
						.get(j + 1);// 后续第二个节点
				if (activityImpl1.getStartTime().equals(
						activityImpl2.getStartTime())) {
					// 如果第一个节点和第二个节点开始时间相同保存
					ActivityImpl sameActivityImpl2 = processDefinitionEntity
							.findActivity(activityImpl2.getActivityId());
					sameStartTimeNodes.add(sameActivityImpl2);
				} else {
					// 有不相同跳出循环
					break;
				}
			}
			List<PvmTransition> pvmTransitions = activityImpl
					.getOutgoingTransitions();// 取出节点的所有出去的线
			for (PvmTransition pvmTransition : pvmTransitions) {
				// 对所有的线进行遍历
				ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition
						.getDestination();
				// 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
				if (sameStartTimeNodes.contains(pvmActivityImpl)) {
					highFlows.add(pvmTransition.getId());
				}
			}
		}
		return highFlows;
	}

}
