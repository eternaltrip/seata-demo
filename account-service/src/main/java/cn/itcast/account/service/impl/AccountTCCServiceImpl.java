package cn.itcast.account.service.impl;

import cn.itcast.account.entity.AccountFreeze;
import cn.itcast.account.mapper.AccountFreezeMapper;
import cn.itcast.account.mapper.AccountMapper;
import cn.itcast.account.service.AccountTCCService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class AccountTCCServiceImpl implements AccountTCCService {



    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountFreezeMapper freezeMapper;



    @Transactional
    @Override
    public void deduct(String userId, int money) {
        //1.获取事务id
        String txid = RootContext.getXID();
        //1.1 判断业务悬挂
        AccountFreeze accountFreeze1 = freezeMapper.selectById(txid);
        if(accountFreeze1 != null ){
            //说明已经执行过了
            return ;
        }


        //2.扣减金额
        accountMapper.deduct(userId,money);
        //3.记录冻结金额，事务状态
        AccountFreeze accountFreeze = new AccountFreeze();
        accountFreeze.setFreezeMoney(money);
        accountFreeze.setXid(txid);
        accountFreeze.setUserId(userId);
        accountFreeze.setState(AccountFreeze.State.TRY);
        freezeMapper.insert(accountFreeze);

    }

    @Override
    public boolean comfirm(BusinessActionContext context) {
        //根据事务id删除冻结记录
        String txid = context.getXid();
        return freezeMapper.deleteById(txid) ==1;
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        //1.获取冻结金额
        String txid = context.getXid();
        String userId = context.getActionContext("userId").toString();
        AccountFreeze accountFreeze = freezeMapper.selectById(txid);
        //2.空回滚判断
        if(accountFreeze == null ){
            //2.1设置空回滚数据
            accountFreeze = new AccountFreeze();
            accountFreeze.setFreezeMoney(0);
            accountFreeze.setXid(txid);
            accountFreeze.setUserId(userId);
            accountFreeze.setState(AccountFreeze.State.CANCEL);
            return freezeMapper.insert(accountFreeze) ==1;
        }
        //2.2判断幂等性
        if (accountFreeze.getState().equals(AccountFreeze.State.CANCEL)){
            //这里已经处理过了，无需再次处理
            return true;
        }

        //3.恢复金额
        accountMapper.refund(accountFreeze.getUserId() , accountFreeze.getFreezeMoney());
        //4.设置空回滚的数据
        accountFreeze.setFreezeMoney(0);
        accountFreeze.setState(AccountFreeze.State.CANCEL);
        return freezeMapper.updateById(accountFreeze) ==1 ;

    }
}
