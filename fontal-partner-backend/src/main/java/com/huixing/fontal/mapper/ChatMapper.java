package com.huixing.fontal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huixing.fontal.model.entity.Chat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper接口
 *
 * @author fontal
 */
@Mapper
public interface ChatMapper extends BaseMapper<Chat> {
}
