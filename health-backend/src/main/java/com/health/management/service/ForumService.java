package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.management.dto.CommentVO;
import com.health.management.dto.ForumDtos.CommentRequest;
import com.health.management.dto.ForumDtos.PostRequest;
import com.health.management.dto.ForumDtos.ReportRequest;
import com.health.management.common.ForumConstants;
import com.health.management.dto.PostVO;
import com.health.management.entity.Comment;
import com.health.management.entity.Post;
import com.health.management.entity.PostLike;
import com.health.management.entity.Report;
import com.health.management.entity.User;
import com.health.management.mapper.CommentMapper;
import com.health.management.mapper.PostLikeMapper;
import com.health.management.mapper.PostMapper;
import com.health.management.mapper.ReportMapper;
import com.health.management.mapper.UserMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ForumService {
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public ForumService(PostMapper postMapper, CommentMapper commentMapper,
                        PostLikeMapper postLikeMapper, ReportMapper reportMapper,
                        UserMapper userMapper,
                        @Lazy NotificationService notificationService) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.postLikeMapper = postLikeMapper;
        this.reportMapper = reportMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    public List<PostVO> listPosts(Long currentUserId, String category, String keyword) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuditStatus, "PUBLISHED")
                .orderByDesc(Post::getCreatedAt);
        if (category != null && !category.isBlank()) {
            query.eq(Post::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(q -> q.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        List<Post> posts = postMapper.selectList(query);
        return buildPostVOs(posts, currentUserId);
    }

    public Post createPost(Long userId, PostRequest request) {
        String category = request.getCategory() == null ? "" : request.getCategory().trim();
        if (!ForumConstants.ALLOWED_POST_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("请选择规范的帖子类型");
        }
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(category);
        post.setTags(request.getTags());
        post.setImageUrl(request.getImageUrl());
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setAuditStatus("PUBLISHED");
        postMapper.insert(post);
        return post;
    }

    public void toggleLike(Long userId, Long postId) {
        PostLike existing = postLikeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId));
        if (existing != null) {
            postLikeMapper.deleteById(existing.getId());
            postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getId, postId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeMapper.insert(like);
            postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getId, postId)
                    .setSql("like_count = like_count + 1"));
            // 发点赞通知给帖子作者
            Post post = postMapper.selectById(postId);
            if (post != null) {
                User liker = userMapper.selectById(userId);
                String likerName = liker != null && liker.getNickname() != null ? liker.getNickname() : "有人";
                notificationService.create(post.getUserId(), "LIKE",
                        likerName + " 赞了你的帖子",
                        "《" + post.getTitle() + "》",
                        "POST", postId, userId);
            }
        }
    }

    public List<CommentVO> listComments(Long postId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getAuditStatus, "PUBLISHED")
                        .orderByAsc(Comment::getCreatedAt));
        return buildCommentVOs(comments);
    }

    public Comment createComment(Long userId, CommentRequest request) {
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setAuditStatus("PUBLISHED");
        commentMapper.insert(comment);
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .eq(Post::getId, request.getPostId())
                .setSql("comment_count = comment_count + 1"));
        // 发评论通知给帖子作者
        Post post = postMapper.selectById(request.getPostId());
        if (post != null) {
            User commenter = userMapper.selectById(userId);
            String name = commenter != null && commenter.getNickname() != null ? commenter.getNickname() : "有人";
            notificationService.create(post.getUserId(), "COMMENT",
                    name + " 评论了你的帖子",
                    "《" + post.getTitle() + "》：" + request.getContent(),
                    "POST", request.getPostId(), userId);
        }
        return comment;
    }

    public Report report(Long userId, ReportRequest request) {
        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return report;
    }

    public void deletePost(Long id) {
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, id));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id));
        postMapper.deleteById(id);
    }

    public void deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            commentMapper.deleteById(id);
            postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getId, comment.getPostId())
                    .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
        }
    }

    // ---- Admin helpers ----

    public List<PostVO> listAllPosts() {
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().orderByDesc(Post::getCreatedAt));
        return buildPostVOs(posts, null);
    }

    public Post auditPost(Long id, String status) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        post.setAuditStatus(status);
        postMapper.updateById(post);
        return post;
    }

    public Comment auditComment(Long id, String status) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        comment.setAuditStatus(status);
        commentMapper.updateById(comment);
        return comment;
    }

    public List<Report> listReports() {
        return reportMapper.selectList(
                new LambdaQueryWrapper<Report>().orderByDesc(Report::getCreatedAt));
    }

    /**
     * 处理举报。action: HIDE=隐藏内容, DELETE=删除内容, IGNORE=不做操作
     */
    public Report handleReport(Long id, Long adminId, String action, String result) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("举报不存在");
        }

        // 根据操作类型处理被举报内容
        String actionDesc = "不做操作";
        if ("HIDE".equals(action)) {
            actionDesc = "已隐藏";
            if ("POST".equals(report.getTargetType())) {
                auditPost(report.getTargetId(), "HIDDEN");
            } else if ("COMMENT".equals(report.getTargetType())) {
                auditComment(report.getTargetId(), "HIDDEN");
            }
        } else if ("DELETE".equals(action)) {
            actionDesc = "已删除";
            if ("POST".equals(report.getTargetType())) {
                deletePost(report.getTargetId());
            } else if ("COMMENT".equals(report.getTargetType())) {
                deleteComment(report.getTargetId());
            }
        }

        report.setStatus("HANDLED");
        report.setHandledBy(adminId);
        report.setResult(result);
        report.setActionTaken(action);
        report.setHandledAt(java.time.LocalDateTime.now());
        reportMapper.updateById(report);

        // 通知举报者处理结果
        String notifContent = "内容类型：" + ("POST".equals(report.getTargetType()) ? "帖子" : "评论")
                + "｜处理操作：" + actionDesc
                + (result != null && !result.isBlank() ? "｜说明：" + result : "");
        notificationService.create(report.getReporterId(), "REPORT_HANDLED",
                "管理员已处理你的举报",
                notifContent,
                "REPORT", id, adminId);

        return report;
    }

    // ---- Private helpers ----

    private List<PostVO> buildPostVOs(List<Post> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper
                .selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> likedPostIds = Set.of();
        if (currentUserId != null) {
            List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            likedPostIds = postLikeMapper.selectList(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getUserId, currentUserId)
                            .in(PostLike::getPostId, postIds))
                    .stream().map(PostLike::getPostId).collect(Collectors.toSet());
        }

        final Set<Long> finalLikedIds = likedPostIds;
        return posts.stream().map(post -> {
            PostVO vo = new PostVO();
            vo.setId(post.getId());
            vo.setUserId(post.getUserId());
            User author = userMap.get(post.getUserId());
            vo.setAuthorNickname(author != null && author.getNickname() != null
                    ? author.getNickname() : "用户" + post.getUserId());
            vo.setAuthorAvatarUrl(author != null ? author.getAvatarUrl() : null);
            vo.setTitle(post.getTitle());
            vo.setContent(post.getContent());
            vo.setCategory(post.getCategory());
            vo.setTags(post.getTags());
            vo.setImageUrl(post.getImageUrl());
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
            vo.setViewCount(post.getViewCount());
            vo.setAuditStatus(post.getAuditStatus());
            vo.setCreatedAt(post.getCreatedAt());
            vo.setLikedByCurrentUser(finalLikedIds.contains(post.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<CommentVO> buildCommentVOs(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper
                .selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            User author = userMap.get(c.getUserId());
            vo.setAuthorNickname(author != null && author.getNickname() != null
                    ? author.getNickname() : "用户" + c.getUserId());
            vo.setAuthorAvatarUrl(author != null ? author.getAvatarUrl() : null);
            vo.setParentId(c.getParentId());
            vo.setContent(c.getContent());
            vo.setLikeCount(c.getLikeCount());
            vo.setCreatedAt(c.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }
}
