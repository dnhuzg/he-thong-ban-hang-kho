<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tạo đơn hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/orders">Đơn hàng</a> / Tạo mới</p>
            <h1>Tạo đơn hàng</h1>
            <p class="page-header__desc">Chọn đại lý để lập đơn. Bạn chỉ thấy các đại lý mình phụ trách.</p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/orders"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
        </div>

        <ol class="steps" aria-label="Các bước tạo đơn">
          <li class="steps__item is-current" aria-current="step"><span class="steps__num">1</span><span><strong>Chọn đại lý</strong><small>Tìm theo mã, tên hoặc số điện thoại</small></span></li>
          <li class="steps__item"><span class="steps__num">2</span><span><strong>Thêm dòng hàng</strong><small>Giá và chiết khấu tự áp theo nhóm đại lý</small></span></li>
          <li class="steps__item"><span class="steps__num">3</span><span><strong>Điểm giao &amp; lưu nháp</strong><small>Chọn điểm giao, ngày giao rồi lưu</small></span></li>
        </ol>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/orders/new" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm đại lý</label>
            <div class="search-field toolbar__search">
              <span class="icon icon--search" aria-hidden="true"></span>
              <input id="q" name="q" type="search" placeholder="Tìm đại lý theo mã, tên hoặc số điện thoại" value="<c:out value='${q}'/>" autofocus>
            </div>
            <button class="btn btn--secondary toolbar__submit toolbar__submit--show" type="submit">Tìm</button>
          </form>

          <c:choose>
            <c:when test="${empty customers.items}">
              <div class="empty-state">
                <span class="empty-state__icon"><span class="icon icon--md icon--building-2" aria-hidden="true"></span></span>
                <p class="empty-state__title"><c:out value="${empty q ? 'Bạn chưa phụ trách đại lý nào' : 'Không có đại lý nào khớp'}"/></p>
                <p class="empty-state__text"><c:out value="${empty q ? 'Liên hệ Quản lý kinh doanh để được phân công đại lý.' : 'Kiểm tra lại mã, tên hoặc số điện thoại rồi tìm lại.'}"/></p>
              </div>
            </c:when>
            <c:otherwise>
              <p class="list-caption"><c:out value="${empty q ? 'Đại lý bạn phụ trách' : 'Kết quả tìm kiếm'}"/> · bấm <strong>Tạo đơn</strong> ở đại lý cần lập đơn.</p>
              <div class="order-picks">
                <c:forEach var="cu" items="${customers.items}">
                  <article class="order-pick">
                    <div class="order-pick__info">
                      <p class="order-pick__name"><c:out value="${cu.name}"/></p>
                      <p class="order-pick__meta"><c:out value="${cu.code}"/><c:if test="${not empty cu.phone}"><c:out value=" · ${cu.phone}"/></c:if> · <c:out value="${empty cu.groupName ? '—' : cu.groupName}"/></p>
                      <c:if test="${cu.locked}"><p class="order-pick__meta"><span class="badge badge--danger">Đang bị khoá giao dịch</span></p></c:if>
                    </div>
                    <c:choose>
                      <c:when test="${cu.locked}">
                        <span class="text-muted">Không tạo được đơn mới</span>
                      </c:when>
                      <c:otherwise>
                        <form action="${ctx}/orders/create" method="post">
                          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                          <input type="hidden" name="customerId" value="${cu.id}">
                          <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Tạo đơn</button>
                        </form>
                      </c:otherwise>
                    </c:choose>
                  </article>
                </c:forEach>
              </div>
              <c:if test="${customers.total > customers.items.size()}">
                <p class="text-muted">Còn <c:out value="${customers.total - customers.items.size()}"/> đại lý khác, gõ cụ thể hơn để thu hẹp.</p>
              </c:if>
            </c:otherwise>
          </c:choose>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
