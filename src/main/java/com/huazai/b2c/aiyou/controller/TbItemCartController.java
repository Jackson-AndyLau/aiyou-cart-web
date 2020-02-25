package com.huazai.b2c.aiyou.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.huazai.b2c.aiyou.pojo.TbItem;
import com.huazai.b2c.aiyou.pojo.TbUser;
import com.huazai.b2c.aiyou.repo.AiyouResultData;
import com.huazai.b2c.aiyou.service.TbItemCartService;
import com.huazai.b2c.aiyou.service.TbItemService;
import com.huazai.b2c.aiyou.service.TbUserService;
import com.huazai.b2c.aiyou.utils.CookieUtils;
import com.huazai.b2c.aiyou.vo.TbItemCartVO;

/**
 * 
 * @author HuaZai
 * @contact who.seek.me@java98k.vip
 *          <ul>
 * @description 商品购物车 Controller
 *              </ul>
 * @className TbItemCartController
 * @package com.huazai.b2c.aiyou.controller
 * @createdTime 2017年06月18日
 *
 * @version V1.0.0
 */
@Controller
@RequestMapping(value = "/item/cart")
public class TbItemCartController
{
	@Autowired
	private TbItemCartService tbItemCartService;

	@Autowired
	private TbUserService tbUserService;

	@Autowired
	private TbItemService tbItemService;

	@Value(value = "${TB_LOGIN_USER_INFO_KEY}")
	private String TB_LOGIN_USER_INFO_KEY;

	@Description(value = "商品添加购物车")
	@RequestMapping(value = "/add/{itemId}")
	public String addTbItemCart(@PathVariable(value = "itemId") Long itemId, Integer num, HttpServletRequest request,
			HttpServletResponse response)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 用户已登录，则将商品添加到系统服务购物车
			TbItem tbItem = tbItemService.getTbItemById(itemId);
			TbItemCartVO tbItemCartVO = new TbItemCartVO();
			BeanUtils.copyProperties(tbItem, tbItemCartVO);

			TbUser tbUser = (TbUser) resultData.getData();
			tbItemCartService.addTbItemCart(tbUser.getId(), tbItemCartVO, num);
		} else
		{
			// 如果用户未登录，则将商品添加到本地Cookie

		}
		return "cartSuccess";
	}

	@Description(value = "显示购物车列表")
	@RequestMapping(value = "/cart")
	public String showTbItemCart(HttpServletRequest request)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 如果用户已登录，则展示服务器上面的购物车列表
			TbUser tbUser = (TbUser) resultData.getData();
			List<TbItemCartVO> tbItemCartVOs = tbItemCartService.queryTbItemByUserId(tbUser.getId());
			request.setAttribute("cartList", tbItemCartVOs);
		} else
		{
			// 如果用户未登录，则展示用户本地Cookie的购物车列表

		}
		return "cart";
	}

	@Description(value = "修改购物车商品数量")
	@RequestMapping(value = "/update/num/{itemId}/{num}")
	public AiyouResultData updateTbItemCart(@PathVariable(value = "itemId") Long itemId,
			@PathVariable(value = "num") Integer num, HttpServletRequest request)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 如果登录，则修改服务器上的购物车商品数量
			TbUser tbUser = (TbUser) resultData.getData();
			tbItemCartService.updateTbItemByUserIdAndItemId(tbUser.getId(), itemId, num);
			return AiyouResultData.ok();
		} else
		{
			// 如果未登录，则修改用户本地Cookie中购物车商品数量

			return AiyouResultData.ok();
		}
	}

}
