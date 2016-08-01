package com.lib.service.user.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lib.dao.UserInfoDao;
import com.lib.dao.UserRegisterDao;
import com.lib.entity.UserInfo;
import com.lib.entity.UserRegister;
import com.lib.service.user.UserRegisterService;
import com.lib.utils.MD5Util;
import com.lib.utils.SendEmail;
import com.lib.utils.StringValueUtil;

@Service
public class UserRegisterServiceImpl implements UserRegisterService {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private UserRegisterDao userRegisterDao;

	/**
	 * 处理注册
	 */
	public void processregister(String userName, String userPassword, String email) {
		UserInfo user = new UserInfo();
		user.setUserName(userName);
		user.setUserPassword(StringValueUtil.getMD5(userPassword));
		user.setUserEmail(email);
		user.setUserType(2);
		userInfoDao.insertUserNoStatus(user);// 保存用户信息
		UserRegister ur = new UserRegister();
		UserInfo user2 = userInfoDao.queryByEmail(email);
		ur.setUserId(user2.getUserId());
		/// 如果处于安全，可以将激活码处理的更复杂点，这里我稍做简单处理
		// user.setValidateCode(MD5Tool.MD5Encrypt(email));
		// user.setValidateCode(MD5Util.encode2hex(email));
		ur.setValidateCode(MD5Util.encode2hex(email));
		ur.setRegisterTime(new Date());
		userRegisterDao.insertNoStatus(ur);

		/// 邮件的内容
		StringBuffer sb = new StringBuffer("点击下面链接激活SOKLIB知识库管理系统网站账号，48小时生效，否则重新注册账号，链接只能使用一次，请尽快激活！</br>");
		sb.append("<a href=\"http://localhost:8080/lib/register?action=activate&email=");
		sb.append(email);
		sb.append("&validateCode=");
		sb.append(ur.getValidateCode());
		sb.append("\">http://localhost:8080/lib/register?action=activate&email=");
		sb.append(email);
		sb.append("&validateCode=");
		sb.append(ur.getValidateCode());
		sb.append("</a>");

		// 发送邮件
		SendEmail.send(email, sb.toString());
		System.out.println("发送邮件");

	}

	/**
	 * 处理激活
	 * 
	 * @throws ParseException
	 */
	/// 传递激活码和email过来
	public void processActivate(String email, String validateCode) throws Exception {
		// 数据访问层，通过email获取用户信息
		UserInfo user = userInfoDao.queryByEmail(email);
		UserRegister ur = userRegisterDao.queryById(user.getUserId());
		// 验证用户是否存在
		if (user != null) {
			// 验证用户激活状态
			if (user.getUserType() == 2) {
				/// 没激活
				Date currentTime = new Date();// 获取当前时间
				// 验证链接是否过期
				currentTime.before(ur.getRegisterTime());
				if (currentTime.before(ur.getLastActivateTime())) {
					// 验证激活码是否正确
					if (validateCode.equals(ur.getValidateCode())) {
						// 激活成功， //并更新用户的激活状态，为已激活
						user.setUserType(1);// 把状态改为激活
						userInfoDao.updateUserNoStatus(user);
						userRegisterDao.updateNoStatus(ur);
					} else {
						LOG.error("激活码不正确");
						throw new Exception("激活码不正确");
					}
				} else {
					LOG.error("激活码已过期！");
					throw new Exception("激活码已过期！");
				}
			} else {
				LOG.error("邮箱已激活，请登录！");
				throw new Exception("邮箱已激活，请登录！");
			}
		} else {
			LOG.error("邮箱已激活，请登录！");
			throw new Exception("该邮箱未注册（邮箱地址不存在）！");
		}

	}

}