package quartz;


import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: zhangh
 * Date: 12-4-26
 * Time: 下午4:45
 * To change this template use File | Settings | File Templates.
 */
public class HelloJob implements Job {
    private static Logger logger = LoggerFactory.getLogger(HelloJob.class) ;
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        //To change body of implemented methods use File | Settings | File Templates.
        logger.info("你好");

    }

    public static void main(String[] args) throws JobExecutionException {
      new HelloJob().execute(null);
    }
}
