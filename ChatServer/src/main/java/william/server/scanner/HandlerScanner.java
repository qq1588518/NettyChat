package william.server.scanner;

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import william.common.core.annotation.SocketCommand;
import william.common.core.annotation.SocketModule;
import william.common.util.EmptyUtil;
import william.common.util.LogUtil;
import william.server.invoker.Invoker;
import william.server.invoker.InvokerHolder;

/**
 * 
 * <p>Description:Handler的扫描器,利用Spring的BeanPostProcessor,在启动时加载所有的Handler</p>
 * <p>虽然使用了反射,但是是在服务器启动时进行处理,对性能没有太大的影响</p>
 * @author ZhangShenao
 * @date 2017年11月29日
 */
@Component
public class HandlerScanner implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		//扫描所有的类,判断是否有@SocketModule和@SocketCommand注解
		Class<?>[] interfaces = bean.getClass().getInterfaces();
		if (EmptyUtil.isEmpty(interfaces)){
			return bean;
		}
		for (Class<?> inter : interfaces){
			SocketModule module = inter.getAnnotation(william.common.core.annotation.SocketModule.class);
			if (null == module){
				continue;
			}
			Method[] methods = inter.getDeclaredMethods();
			if (EmptyUtil.isEmpty(methods)){
				continue;
			}
			for (Method method : methods){
				SocketCommand command = method.getAnnotation(william.common.core.annotation.SocketCommand.class);
				if (null == command){
					continue;
				}
				
				//注册Invoker实例
				short moduleId = module.module();
				short cmdId = command.cmd();
				Invoker invoker = InvokerHolder.getInvoker(moduleId, cmdId);
				if (null != invoker){
					LogUtil.error("扫描Handler时,Handler被重复注册!!");
				}
				else {
					invoker = Invoker.getInvoker(method, bean);
					InvokerHolder.addInvoker(invoker, moduleId, cmdId);
					LogUtil.info("注册Invoker,moduleId: " + moduleId + ",cmdId: " + cmdId);
				}
			}
		}
		return bean;
	}

}
