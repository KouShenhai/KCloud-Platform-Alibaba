package io.laokou.admin.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.laokou.admin.interfaces.dto.MessageDTO;
import io.laokou.admin.interfaces.qo.MessageQO;
import io.laokou.admin.interfaces.vo.MessageDetailVO;
import io.laokou.admin.interfaces.vo.MessageVO;

import javax.servlet.http.HttpServletRequest;

public interface SysMessageApplicationService {

    Boolean pushMessage(MessageDTO dto);

    Boolean sendMessage(MessageDTO dto, HttpServletRequest request);

    void consumeMessage(MessageDTO dto);

    Boolean insertMessage(MessageDTO dto);

    IPage<MessageVO> queryMessagePage(MessageQO qo);

    MessageDetailVO getMessageById(Long id);

    IPage<MessageVO> getUnReadList(HttpServletRequest request, MessageQO qo);

    Boolean readMessage(Long id);

    Integer unReadCount(HttpServletRequest request);

}
