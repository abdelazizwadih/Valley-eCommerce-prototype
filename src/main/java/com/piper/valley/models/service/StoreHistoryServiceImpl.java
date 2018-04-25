package com.piper.valley.models.service;

import com.piper.valley.auth.AuthService;
import com.piper.valley.auth.CurrentUser;
import com.piper.valley.forms.AddStoreCollaboratorForm;
import com.piper.valley.forms.AddStoreProductForm;
import com.piper.valley.models.domain.*;
import com.piper.valley.models.repository.StoreHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public class StoreHistoryServiceImpl implements  StoreHistoryService{
    @Autowired
    StoreHistoryRepository storeHistoryRepository;

    @Autowired
	AuthService authService;

	@Autowired
	StoreService storeService;

	@Autowired
	StoreProductService storeProductService;

    @Override
    public void add(StoreHistory storeHistory)
    {
        storeHistoryRepository.save(storeHistory);
    }

	@Override
	public Optional<StoreHistory> getById(Long Id) {
    	return storeHistoryRepository.findOneById(Id);
	}

	@Override
	public Boolean undo(Long HistoryId, CurrentUser currentUser) {
		StoreHistory storeHistory = storeHistoryRepository.findOne(HistoryId);

		if(storeHistory == null)
			return false;

		if(storeHistory instanceof StoreCollabHistory)
			return undoCollab((StoreCollabHistory) storeHistory, currentUser);

		if(storeHistory instanceof StoreProductHistory)
			return undoProduct((StoreProductHistory) storeHistory, currentUser);

		return false;
	}

	@Override
	public Boolean undoCollab(StoreCollabHistory storeCollabHistory, CurrentUser currentUser) {
		if(!authService.canAccessStore(storeCollabHistory.getStore(), currentUser))
			return false;

		if(storeCollabHistory.getType() == StoreHistoryType.ADD)
			storeService.removeCollaboratorToStore(
				new AddStoreCollaboratorForm(storeCollabHistory.getStore().getId(),
				storeCollabHistory.getCollab().getUsername()), currentUser.getId()
			);

		else if(storeCollabHistory.getType() == StoreHistoryType.DELETE)
			storeService.addCollaboratorToStore(
					new AddStoreCollaboratorForm(storeCollabHistory.getStore().getId(),
					storeCollabHistory.getCollab().getUsername()), currentUser.getId()
			);


		return true;
	}

	@Override
		public Boolean undoProduct(StoreProductHistory storeProductHistory, CurrentUser currentUser) {

		if(!authService.canAccessStore(storeProductHistory.getStore(), currentUser))
			return false;

		if(storeProductHistory.getType() == StoreHistoryType.DELETE) {
			//TODO make this better (via shadow-deleting to preserve orders, etc)
			storeService.addProductToStore(
					new AddStoreProductForm(
							storeProductHistory.getStoreId(),
							storeProductHistory.getProductId(),
							storeProductHistory.getOldProductName(),
							storeProductHistory.getOldProductDescription(),
							storeProductHistory.getOldProductPrice()
					), currentUser.getUser());
			return true;
		}

		Optional<StoreProduct> storeProduct = storeProductService.getProductById(storeProductHistory.getStoreProductID());

		if(!storeProduct.isPresent())
			return false;

		if(storeProductHistory.getType() == StoreHistoryType.ADD)
			storeService.removeProductFromStore(storeProduct.get().getId(), currentUser.getUser());

		return true;
	}

	@Override
    public Collection<StoreHistory> getByStoreID(Long id)
    {
        return  storeHistoryRepository.findAllByStoreId(id);
    }
}
