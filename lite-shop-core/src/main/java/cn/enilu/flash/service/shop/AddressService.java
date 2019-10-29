package cn.enilu.flash.service.shop;


import cn.enilu.flash.bean.entity.shop.Address;
import cn.enilu.flash.dao.shop.AddressRepository;

import cn.enilu.flash.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddressService extends BaseService<Address,Long,AddressRepository>  {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private AddressRepository addressRepository;

}

