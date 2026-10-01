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
        </div>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/orders/new" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm đại lý</label>
            <div class="search-field toolbar__search">
              <span class="icon icon--search" aria-hidden="true"></span>
              <input id="q" name="q" type="search" placeholder="Tìm đại lý theo mã, tên hoặc số điện thoại" value="<c:out value='${q}'/>" autofocus>
            </div>
            <button class="btn btn--secondary toolbar__submit" type="submit">Tìm</button>
          </form>

          <c:choose>
            <c:when test="${customers == null}">
              <p class="text-muted">Nhập mã, tên hoặc số điện thoại đại lý rồi bấm Tìm.</p>
            </c:when>
            <c:when test="${empty customers.items}">
              <p class="text-muted">Không có đại lý nào khớp.</p>
            </c:when>
            <c:otherwise>
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
                          <button class="btn btn--primary" type="submit">Tạo đơn</button>
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
