package org.laokou.admin.command.user;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.laokou.admin.convertor.UserConvertor;
import org.laokou.admin.domain.gateway.UserGateway;
import org.laokou.admin.domain.user.User;
import org.laokou.admin.dto.user.UserUpdateCmd;
import org.laokou.admin.dto.user.clientobject.UserCO;
import org.laokou.admin.gatewayimpl.database.UserMapper;
import org.laokou.admin.gatewayimpl.database.dataobject.UserDO;
import org.laokou.common.i18n.common.exception.SystemException;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.crypto.utils.AesUtil;
import org.laokou.common.security.utils.UserUtil;
import org.springframework.stereotype.Component;

import static org.laokou.common.i18n.common.DatasourceConstants.TENANT;

/**
 * @author laokou
 */
@Component
@RequiredArgsConstructor
public class UserUpdateCmdExe {

	private final UserGateway userGateway;

	private final UserMapper userMapper;

	private final UserConvertor userConvertor;

	@DS(TENANT)
	public Result<Boolean> execute(UserUpdateCmd cmd) {
		UserCO co = cmd.getUserCO();
		// 用户表
		int count = userMapper.getUserCount(toUserDO(co), AesUtil.getKey());
		if (count > 0) {
			throw new SystemException("用户名已存在，请重新输入");
		}
		return Result.of(userGateway.update(toUser(co)));
	}

	private UserDO toUserDO(UserCO co) {
		return userConvertor.toDataObj(co);
	}

	private User toUser(UserCO co) {
		User user = userConvertor.toEntity(co);
		user.setTenantId(UserUtil.getTenantId());
		user.setCreator(UserUtil.getUserId());
		user.setEditor(UserUtil.getUserId());
		user.setDeptId(co.getDeptId());
		user.setDeptPath(co.getDeptPath());
		return user;
	}

}
