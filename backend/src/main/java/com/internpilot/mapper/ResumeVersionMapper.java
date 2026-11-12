package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.ResumeVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeVersionMapper extends BaseMapper<ResumeVersion> {
}
// 该接口继承了MyBatis-Plus的BaseMapper，提供了对ResumeVersion实体的基本CRUD操作，无需编写实现类。
// 通过@Mapper注解，MyBatis会自动扫描并生成对应的实现类，使得我们可以直接在Service层注入ResumeVersionMapper来使用这些方法。
// 在实际使用中，我们只需要在Mapper接口中定义需要的方法，MyBatis-Plus会自动生成对应的SQL语句并执行。
//CRUD操作包括：
//1. 插入数据：insert(ResumeVersion resumeVersion)

//2. 更新数据：updateById(ResumeVersion resumeVersion)
//3. 根据ID查询数据：selectById(Long id)

//4. 删除数据：deleteById(Long id)
//5. 查询所有数据：selectList(null)
//6. 根据条件查询数据：selectList(Wrapper<ResumeVersion> queryWrapper)
//7. 分页查询数据：selectPage(Page<ResumeVersion> page, Wrapper<ResumeVersion> queryWrapper)
//8. 根据ID批量删除数据：deleteBatchIds(Collection<? extends Serializable> idList)
//9. 根据条件批量删除数据：delete(Wrapper<ResumeVersion> queryWrapper)
//10. 根据ID查询数据，并返回Map：selectMapById(Long id)
//11. 根据条件查询数据，并返回Map：selectMaps(Wrapper<ResumeVersion> queryWrapper)
//12. 根据条件查询数据，并返回Map，只返回第一个结果：selectMap(Wrapper<ResumeVersion> queryWrapper)
//13. 根据条件查询数据，并返回对象列表：selectObjs(Wrapper<ResumeVersion> queryWrapper)
//14. 根据条件查询数据，并返回对象列表，只返回第一个结果：selectObj

//15. 根据条件查询数据，并返回分页对象：selectPage(Page<ResumeVersion> page, Wrapper<ResumeVersion> queryWrapper)
//16. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectOne(Wrapper<ResumeVersion> queryWrapper)
//17. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectCount
//18. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectList(Wrapper<ResumeVersion> queryWrapper)
//19. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectBatchIds(Collection<? extends Serializable> idList)
//20. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectByMap(Map<String, Object> columnMap)
//21. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectByIds(Collection<? extends Serializable> idList)
//22. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectByMap(Map<String, Object> columnMap)
//23. 根据条件查询数据，并返回分页对象，只返回第一个结果：selectById(Long id)