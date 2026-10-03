package com.edu.course.service;

import com.edu.course.entity.Notice;
import com.edu.course.entity.User;
import com.edu.course.mapper.NoticeMapper;
import com.edu.course.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 公告服务
 */
@Slf4j
@Service
public class NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    /**
     * 获取所有公告（带缓存）
     */
    @Cacheable(value = "noticeList")
    public List<Notice> getAllNotices() {
        log.info("🔴 [Redis] 缓存未命中 noticeList，从 MySQL 查询并写入 Redis");
        return noticeMapper.selectList(null);
    }

    /** 根据ID获取公告 */
    public Notice getById(Integer id) {
        return noticeMapper.selectById(id);
    }

    /**
     * 新增公告 → 清缓存 + 推送消息给所有学生
     */
    @CacheEvict(value = "noticeList", allEntries = true)
    public void addNotice(Notice notice) {
        notice.setCreateTime(new Date());
        notice.setUpdateTime(new Date());
        noticeMapper.insert(notice);
        log.info("🔴 [Redis] 清除缓存 noticeList（原因：新增公告「{}」）", notice.getTitle());

        // 推送站内消息给所有学生
        pushToAllStudents("📢 新公告：「" + notice.getTitle() + "」",
                "系统发布了新公告，标题：" + notice.getTitle() + "\n"
                + "发布者：" + (notice.getAuthor() != null ? notice.getAuthor() : "系统") + "\n"
                + "请前往公告查看页面了解详情。");
    }

    /**
     * 更新公告 → 清缓存 + 推送消息
     */
    @CacheEvict(value = "noticeList", allEntries = true)
    public void updateNotice(Notice notice) {
        notice.setUpdateTime(new Date());
        noticeMapper.updateById(notice);
        log.info("🔴 [Redis] 清除缓存 noticeList（原因：更新公告「{}」）", notice.getTitle());

        pushToAllStudents("📝 公告已更新：「" + notice.getTitle() + "」",
                "公告「" + notice.getTitle() + "」内容已更新，请前往查看。");
    }

    /**
     * 删除公告 → 清缓存
     */
    @CacheEvict(value = "noticeList", allEntries = true)
    public void deleteNotice(Integer id) {
        Notice notice = noticeMapper.selectById(id);
        noticeMapper.deleteById(id);
        if (notice != null) {
            log.info("🔴 [Redis] 清除缓存 noticeList（原因：删除公告「{}」）", notice.getTitle());
        }
    }

    /** 推送消息给所有学生 */
    private void pushToAllStudents(String title, String content) {
        List<User> students = userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getRole, 2)          // 角色=学生
                        .eq(User::getDisabled, 0));    // 未被禁用
        for (User student : students) {
            messageService.sendMessage(student.getId(), title, content);
        }
        log.info("📬 [消息推送] 公告消息已推送给 {} 名学生", students.size());
    }
}
