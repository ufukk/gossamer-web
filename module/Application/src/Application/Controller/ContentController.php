<?php
/**
 * Zend Framework (http://framework.zend.com/)
 *
 * @link      http://github.com/zendframework/ZendSkeletonApplication for the canonical source repository
 * @copyright Copyright (c) 2005-2015 Zend Technologies USA Inc. (http://www.zend.com)
 * @license   http://framework.zend.com/license/new-bsd New BSD License
 */

namespace Application\Controller;

use Zend\Mvc\Controller\AbstractActionController;
use Zend\View\Model\ViewModel;
use Elasticsearch\ClientBuilder;

class ContentController extends AbstractActionController
{

    private $sources = array(
        array('title' => 'Facebook', 'id' => 'facebook'),
        array('title' => 'Twitter', 'id' => 'twitter'),
        array('title' => 'EkşiSözlük', 'id' => 'eksisozluk'),
        array('title' => 'RSS', 'id' => 'rss')
      );

    public function indexAction()
    {
        return new ViewModel();
    }

    public function statsAction() 
    {
		$this->layout()->setVariable('currentUri', 'stats');
		$result = new ViewModel();
		return $result;
    }

    private function buildQuery($body, $type, $options = array()) 
    {
      $config = $this->getServiceLocator()->get('config')['datastore'];
      return array_merge(array('index' => $config['index'], 'type' => $type, 'body' => $body), $options);
    }

    private function buildFilterQuery($terms, $type, $required = false, $options = array()) 
    {
      $key = $required ? 'must' : 'should';
      $body = array('query' => array(
          'filtered' => array(
            'filter' => array(
              'bool' => array(
                $key => $terms
              )
            )
          )
        )
      );

      return $this->buildQuery($body, $type, $options);
    }

    public function statsPaneAction() 
    {
      
      $client = ClientBuilder::create()->build();
      
      $numbers = array();
      $contents = array();
      foreach ($this->sources as $source) {
        $cursorCount = $client->count($this->buildFilterQuery(array('term' => array('source' => $source['id'])), 'cursors'))['count'];
        $contentCount = $client->count($this->buildFilterQuery(array('term' => array('source' => $source['id'])), 'contents'))['count'];
        $contentQuery = $this->buildFilterQuery(array('term' => array('source' => $source['id'])), 'contents');
        $contentQuery['body']['size'] = 5;
        $contentQuery['body']['sort'] = array('collectedAt' => 'desc');
        $freshContents = $client->search($contentQuery);
        $numbers[$source['title']] = array('cursors' => $cursorCount, 'contents' => $contentCount);
        $contents[$source['title']] = $freshContents;
      }

      $result = new ViewModel();
      
      $result->setVariables(array('sources' => $this->sources, 'numbers' => $numbers, 'contents' => $contents));
      $result->setTerminal(true);
      return $result; 
    }

    public function searchAction()
    {
		$this->layout()->setVariable('currentUri', 'list');
		$result = new ViewModel();
		return $result;
    }

    public function searchFormPaneAction()
    {
      $result = new ViewModel();
      $client = ClientBuilder::create()->build();
      $cursorQuery = $this->buildFilterQuery(array('term' => array('type' => 'keyword')), 'cursors', true, array('size' => 1000));
      $cursors = $client->search($cursorQuery);
      $keywords = array();
      if(count($cursors) > 0) {
        foreach($cursors['hits']['hits'] as $cursor) {
          $keywords[$cursor['_source']['locationId']] = $cursor['_source']['locationId'];
        }
        $result->setVariables(array('keywords' => $keywords));
      }
      $result->setTerminal(true);
      return $result;
    }

    public function searchResultsPaneAction()
    {
      $request = $this->getRequest()->getQuery();
      $result = new ViewModel();
      $keywords = explode(',', $request['keywords']);
      $from = isset($request['from']) ? $request['from'] : 0;
      $limit = isset($request['limit']) ? $request['limit'] : 10;

      $query = array('query' => array('bool' => array('should' => array())), 'from' => $from, 'size' => $limit);
      foreach($keywords as $keyword) {
        $query['query']['bool']['should'][] = array('match' => array('body' => $keyword));
      }
      $client = ClientBuilder::create()->build();
      $contents = $client->search($this->buildQuery($query, 'contents'));
	  
	  $total = $contents["hits"]["total"];
	  
	  $pageTotal = ceil($total / $limit);
	  $pageNow = ceil($from / 10);
	  
	  $start = $pageNow - 5 < 0 ? 0 : $pageNow - 5;
	  
	  $paginationLinks = array();
	  for($start; ($start < $pageTotal) && ($start < $pageNow + 5); $start++) $paginationLinks[] = $start;
	  
      if(count($contents) > 0) {
        $result->setVariables(array(
			"contents" => $contents['hits']['hits'],
			"total" => $total,
			"paginationLinks" => $paginationLinks,
			"pageNow" => $pageNow,
			"pageTotal" => $pageTotal
		));  
      }
      $result->setTerminal(true);
      return $result;
    }

    public function keywordsAction() {
		$this->layout()->setVariable('currentUri', 'keywords');
		$result = new ViewModel();
		return $result;
    }

    public function keywordListPaneAction() 
    {
      $request = $this->getRequest()->getQuery();
      $result = new ViewModel();
      $client = ClientBuilder::create()->build();
      $cursorQuery = $this->buildFilterQuery(array('term' => array('type' => 'keyword')), 'cursors', true, array('size' => 1000));
      $cursors = $client->search($cursorQuery);
      $keywords = array();
      if(count($cursors) > 0) {
        foreach($cursors['hits']['hits'] as $cursor) {
          $keywords[$cursor['_source']['locationId']] = $cursor['_source']['locationId'];
        }
        $result->setVariables(array('keywords' => $keywords));
      }
      $result->setTerminal(true);
      return $result;
    }

    public function removeKeywordAction() 
    {
      $keyword = $this->params()->fromQuery('keyword');
      $client = ClientBuilder::create()->build();
      $query = $this->buildFilterQuery(array('term' => array('type' => 'keyword', 'locationId' => $keyword)), 'cursors');
      $client->deleteByQuery($query);
      return $this->forward()->dispatch('Application\Controller\Content', array('action' => 'keywordListPane'));
    }

    public function keywordFormPaneAction()
    {
      $result = new ViewModel();
      $result->setTerminal(true);
      return $result;
    }

    public function addKeywordAction() 
    {
      $keyword = $this->params()->fromQuery('keyword');
      $client = ClientBuilder::create()->build();
      $result = new ViewModel();
      $baseQuery = $this->buildQuery(array(), 'cursors');
      foreach($this->sources as $source) {
        $cursorId = "{$source['id']}_{$keyword}";
        $cursorResult = $client->count($this->buildFilterQuery(array('term' => array('id' => $cursorId)), 'cursors'));
        if($cursorResult['count'] == 0) {
          $body = array('source' => $source['id'], 'type' => 'keyword', 'id' => $cursorId, 'locationId' => $keyword, 'priority' => 1, 'lastCollectedAt' => 0, 'readOrder' => 1);
          $query = $this->buildQuery($body, 'cursors');
          $query['id'] = $cursorId;
          $client->index($query);
        }        
      }
      return $this->forward()->dispatch('Application\Controller\Content', array('action' => 'keywordListPane'));
    }


}
