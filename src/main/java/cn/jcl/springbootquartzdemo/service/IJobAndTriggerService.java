package cn.jcl.springbootquartzdemo.service;


import cn.jcl.springbootquartzdemo.vo.JobAndTrigger;
import com.github.pagehelper.PageInfo;

public interface IJobAndTriggerService {
	public PageInfo<JobAndTrigger> getJobAndTriggerDetails(int pageNum, int pageSize);
}
